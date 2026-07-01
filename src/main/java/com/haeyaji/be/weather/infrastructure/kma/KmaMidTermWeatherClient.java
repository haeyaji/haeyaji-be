package com.haeyaji.be.weather.infrastructure.kma;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.haeyaji.be.common.exception.BusinessException;
import com.haeyaji.be.common.exception.ErrorCode;
import com.haeyaji.be.weather.application.port.out.MidTermWeatherProvider;
import com.haeyaji.be.weather.domain.Weather;
import com.haeyaji.be.weather.domain.WeatherCondition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 중기예보 아웃바운드 어댑터. 두 소스를 합쳐 하루 단위 {@link Weather} 로 매핑한다.
 * <ul>
 *   <li>중기육상예보(getMidLandFcst, JSON): 하늘상태(wf)·강수확률(rnSt)</li>
 *   <li>중기기온(fct_afs_wc.php, 텍스트): 최저/최고기온</li>
 * </ul>
 * 시간별·습도·바람·체감은 중기예보에 없으므로 null/빈값.
 */
@Slf4j
@Component
public class KmaMidTermWeatherClient implements MidTermWeatherProvider {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final WebClient landClient;
    private final WebClient taClient;
    private final ObjectMapper objectMapper;
    private final String authKey;

    public KmaMidTermWeatherClient(ObjectMapper objectMapper,
                                   @Value("${haeyaji.weather.kma.mid.land-base-url}") String landBaseUrl,
                                   @Value("${haeyaji.weather.kma.mid.ta-base-url}") String taBaseUrl,
                                   @Value("${haeyaji.weather.kma.auth-key:}") String authKey) {
        // 서로 다른 baseUrl 두 개라 각각 독립 빌더로 생성 (공용 빌더 변형/deprecated clone 회피).
        this.landClient = WebClient.builder().baseUrl(landBaseUrl).build();
        this.taClient = WebClient.builder().baseUrl(taBaseUrl).build();
        this.objectMapper = objectMapper;
        this.authKey = authKey;
    }

    @Override
    public Weather fetch(double lat, double lng, LocalDate date) {
        if (!StringUtils.hasText(authKey)) {
            log.error("KMA auth key is not configured (KMA_AUTH_KEY)");
            throw new BusinessException(ErrorCode.WEATHER_UPSTREAM_ERROR);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate baseDate = KmaMidBaseTime.baseDate(now);
        String tmFc = KmaMidBaseTime.resolveTmFc(now);
        int n = (int) ChronoUnit.DAYS.between(baseDate, date); // wf 인덱스 (4~10)
        if (n < 4 || n > 10) {
            // 06시 발표 기준 제공 범위(+4~+10) 밖. 이른 새벽 +10 요청 등 드문 경계.
            log.warn("mid-term out of range: wf index n={} (date={})", n, date);
            throw new BusinessException(ErrorCode.WEATHER_UPSTREAM_ERROR);
        }

        MidTermRegion region = MidTermRegion.nearest(lat, lng);

        JsonNode land = callLand(region.landRegId(), tmFc);
        MinMax temp = callTemp(region.taRegId(), tmFc, date);

        return mapToWeather(land, temp, n);
    }

    private Weather mapToWeather(JsonNode landItem, MinMax temp, int n) {
        // n 4~7: 오전/오후 구분(Am/Pm), n 8~10: 단일
        String wf;
        int pop;
        if (n <= 7) {
            wf = text(landItem, "wf" + n + "Pm");
            pop = Math.max(intOr(landItem, "rnSt" + n + "Am", 0), intOr(landItem, "rnSt" + n + "Pm", 0));
        } else {
            wf = text(landItem, "wf" + n);
            pop = intOr(landItem, "rnSt" + n, 0);
        }

        if (wf == null && temp == null) {
            log.error("mid-term forecast missing for wf index n={}", n);
            throw new BusinessException(ErrorCode.WEATHER_UPSTREAM_ERROR);
        }

        int hi = temp != null ? temp.max() : 0;
        int lo = temp != null ? temp.min() : 0;
        int rep = temp != null ? Math.round((temp.min() + temp.max()) / 2.0f) : 0;

        return Weather.builder()
                .cond(WeatherCondition.fromMidLandText(wf))
                .condKo(wf != null ? wf : "정보 없음")
                .temp(rep)
                .hi(hi)
                .lo(lo)
                .pop(pop)
                .feels(null)      // 중기예보 미제공
                .humidity(null)   // 중기예보 미제공
                .windMs(null)     // 중기예보 미제공
                .uvIndex(null)
                .pm10(null)
                .pm25(null)
                .hourly(List.of())
                .build();
    }

    /** 중기육상예보 item 노드. */
    private JsonNode callLand(String regId, String tmFc) {
        try {
            String body = landClient.get()
                    .uri(uri -> uri.path("/getMidLandFcst")
                            .queryParam("authKey", authKey)
                            .queryParam("dataType", "JSON")
                            .queryParam("numOfRows", 10)
                            .queryParam("pageNo", 1)
                            .queryParam("regId", regId)
                            .queryParam("tmFc", tmFc)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            JsonNode root = objectMapper.readTree(body);
            String resultCode = root.path("response").path("header").path("resultCode").asString("");
            if (!"00".equals(resultCode)) {
                log.error("mid land upstream error: {}", body != null && body.length() > 200 ? body.substring(0, 200) : body);
                throw new BusinessException(ErrorCode.WEATHER_UPSTREAM_ERROR);
            }
            JsonNode item = root.path("response").path("body").path("items").path("item");
            if (item.isArray() && !item.isEmpty()) {
                return item.get(0);
            }
            log.error("mid land response has no item (regId={})", regId);
            throw new BusinessException(ErrorCode.WEATHER_UPSTREAM_ERROR);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("mid land call failed (regId={})", regId, e);
            throw new BusinessException(ErrorCode.WEATHER_UPSTREAM_ERROR, e);
        }
    }

    /**
     * 중기기온(fct_afs_wc.php) 텍스트에서 대상 날짜의 최저/최고기온 파싱.
     * 컬럼: REG_ID TM_FC TM_EF MOD STN C MIN MAX ...
     */
    private MinMax callTemp(String regId, String tmFc, LocalDate date) {
        String targetYmd = date.format(YMD);
        try {
            String body = taClient.get()
                    .uri(uri -> uri.path("/fct_afs_wc.php")
                            .queryParam("reg", regId)
                            .queryParam("tmfc1", tmFc)
                            .queryParam("tmfc2", tmFc)
                            .queryParam("tmef1", targetYmd)
                            .queryParam("tmef2", targetYmd)
                            .queryParam("disp", 0)
                            .queryParam("help", 0)
                            .queryParam("authKey", authKey)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            if (body == null) {
                return null;
            }
            for (String line : body.split("\\R")) {
                String s = line.trim();
                if (s.isEmpty() || s.startsWith("#")) {
                    continue;
                }
                String[] c = s.split("\\s+");
                // REG_ID TM_FC TM_EF MOD STN C MIN MAX
                if (c.length < 8 || !c[2].startsWith(targetYmd) || !"A01".equals(c[3])) {
                    continue;
                }
                Integer min = parseIntOrNull(c[6]);
                Integer max = parseIntOrNull(c[7]);
                if (min != null && max != null) {
                    return new MinMax(min, max);
                }
            }
            log.warn("mid temp not found for regId={} date={}", regId, targetYmd);
            return null;
        } catch (Exception e) {
            log.error("mid temp call failed (regId={})", regId, e);
            throw new BusinessException(ErrorCode.WEATHER_UPSTREAM_ERROR, e);
        }
    }

    private record MinMax(int min, int max) {
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asString();
    }

    private static int intOr(JsonNode node, String field, int fallback) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? fallback : v.asInt(fallback);
    }

    private static Integer parseIntOrNull(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
