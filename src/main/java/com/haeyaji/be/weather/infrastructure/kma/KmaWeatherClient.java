package com.haeyaji.be.weather.infrastructure.kma;

import com.haeyaji.be.common.exception.BusinessException;
import com.haeyaji.be.common.exception.ErrorCode;
import com.haeyaji.be.weather.application.port.out.ShortTermWeatherProvider;
import com.haeyaji.be.weather.domain.FeelsLike;
import com.haeyaji.be.weather.domain.HourlyWeather;
import com.haeyaji.be.weather.domain.Weather;
import com.haeyaji.be.weather.domain.WeatherCondition;
import com.haeyaji.be.weather.infrastructure.kma.KmaForecastResponse.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 기상청 단기예보(getVilageFcst) 아웃바운드 어댑터.
 * 좌표→격자 변환 후 실제 API를 호출하고, 원시 응답을 {@link Weather} 도메인으로 매핑한다.
 *
 * <p>uvIndex/pm10/pm25 는 기상청 생활기상지수·에어코리아 등 별도 서비스라 이 어댑터에선 null.
 * (후속 이슈에서 별도 provider 합성 예정)
 */
@Slf4j
@Component
public class KmaWeatherClient implements ShortTermWeatherProvider {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    // 단기예보 전체 항목(totalCount ~1016)을 한 페이지로 받도록 여유 있게.
    private static final int NUM_OF_ROWS = 1200;
    private static final int PREFERRED_HOUR_FUTURE = 1200;
    private static final int MAX_HOURLY = 8;

    private final WebClient webClient;
    private final String authKey;

    public KmaWeatherClient(WebClient.Builder webClientBuilder,
                            @Value("${haeyaji.weather.kma.base-url}") String baseUrl,
                            @Value("${haeyaji.weather.kma.auth-key:}") String authKey) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.authKey = authKey;
    }

    @Override
    public Weather fetch(double lat, double lng, LocalDate date) {
        if (!StringUtils.hasText(authKey)) {
            log.error("KMA auth key is not configured (KMA_AUTH_KEY)");
            throw new BusinessException(ErrorCode.WEATHER_UPSTREAM_ERROR);
        }

        GridConverter.Grid grid = GridConverter.toGrid(lat, lng);
        LocalDateTime now = LocalDateTime.now();

        // 늦은 밤(예: 23시대)엔 최신 발표(2300)의 첫 예보가 익일 0시라 '오늘' 데이터가 없다.
        // 대상 날짜 예보가 비면 이전 발표(3시간 전 슬롯)로 순차 폴백한다.
        for (int slotsBack = 0; slotsBack <= 2; slotsBack++) {
            KmaBaseTime.BaseTime base = KmaBaseTime.resolve(now.minusHours(3L * slotsBack));
            KmaForecastResponse response = callApi(grid, base);
            if (response == null || !response.isSuccess()) {
                String msg = response == null || response.response() == null || response.response().header() == null
                        ? "no response"
                        : response.response().header().resultMsg();
                log.error("KMA upstream error: {}", msg);
                throw new BusinessException(ErrorCode.WEATHER_UPSTREAM_ERROR);
            }
            Weather weather = mapOrNull(response.items(), date);
            if (weather != null) {
                return weather;
            }
            log.debug("no forecast for date={} at base {} {}, retrying earlier",
                    date, base.baseDate(), base.baseTime());
        }
        log.error("KMA has no forecast for date={} after base-time fallback", date);
        throw new BusinessException(ErrorCode.WEATHER_UPSTREAM_ERROR);
    }

    private KmaForecastResponse callApi(GridConverter.Grid grid, KmaBaseTime.BaseTime base) {
        try {
            return webClient.get()
                    .uri(uri -> uri.path("/getVilageFcst")
                            .queryParam("authKey", authKey)
                            .queryParam("dataType", "JSON")
                            .queryParam("numOfRows", NUM_OF_ROWS)
                            .queryParam("pageNo", 1)
                            .queryParam("base_date", base.baseDate())
                            .queryParam("base_time", base.baseTime())
                            .queryParam("nx", grid.nx())
                            .queryParam("ny", grid.ny())
                            .build())
                    .retrieve()
                    .bodyToMono(KmaForecastResponse.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
        } catch (Exception e) {
            // 인증 실패 시 data.go.kr는 XML 에러 바디를 반환 → JSON 파싱 실패 포함
            log.error("KMA call failed (nx={}, ny={})", grid.nx(), grid.ny(), e);
            throw new BusinessException(ErrorCode.WEATHER_UPSTREAM_ERROR, e);
        }
    }

    /**
     * 대상 날짜의 예보 항목을 시각별로 정리해 대표값·시간별 예보로 축약한다.
     * 대상 날짜 예보가 없으면 null(호출자가 이전 발표로 폴백).
     */
    private Weather mapOrNull(List<Item> items, LocalDate date) {
        String target = date.format(DATE);

        // fcstTime(정수) → category → value
        TreeMap<Integer, Map<String, String>> byTime = new TreeMap<>();
        Integer tmx = null;
        Integer tmn = null;
        for (Item item : items) {
            if (!target.equals(item.fcstDate())) {
                continue;
            }
            int time = parseIntSafe(item.fcstTime(), -1);
            if (time < 0) {
                continue;
            }
            byTime.computeIfAbsent(time, k -> new java.util.HashMap<>())
                    .put(item.category(), item.fcstValue());

            if ("TMX".equals(item.category())) {
                tmx = roundSafe(item.fcstValue());
            } else if ("TMN".equals(item.category())) {
                tmn = roundSafe(item.fcstValue());
            }
        }

        if (byTime.isEmpty()) {
            return null;
        }

        int repTime = pickRepresentativeTime(byTime, date);
        Map<String, String> rep = byTime.get(repTime);

        int temp = roundSafeOr(rep.get("TMP"), 0);
        int pop = parseIntSafe(rep.get("POP"), 0);
        int humidity = parseIntSafe(rep.get("REH"), 0);
        double windMs = parseDoubleSafe(rep.get("WSD"), 0.0);
        int sky = parseIntSafe(rep.get("SKY"), 1);
        int pty = parseIntSafe(rep.get("PTY"), 0);

        int hi = tmx != null ? tmx : maxTemp(byTime, temp);
        int lo = tmn != null ? tmn : minTemp(byTime, temp);
        int feels = FeelsLike.calculate(temp, humidity, windMs);

        return Weather.builder()
                .cond(WeatherCondition.resolve(sky, pty))
                .condKo(WeatherCondition.describeKo(sky, pty))
                .temp(temp)
                .hi(hi)
                .lo(lo)
                .feels(feels)
                .pop(pop)
                .humidity(humidity)
                .windMs(windMs)
                .uvIndex(null)
                .pm10(null)
                .pm25(null)
                .hourly(buildHourly(byTime, repTime))
                .build();
    }

    /**
     * 대표 시각 선택: 오늘이면 현재 시각 이후 가장 이른 예보(없으면 마지막),
     * 미래 날짜면 정오(없으면 첫 예보).
     */
    private int pickRepresentativeTime(TreeMap<Integer, Map<String, String>> byTime, LocalDate date) {
        if (date.isEqual(LocalDate.now())) {
            int nowSlot = LocalDateTime.now().getHour() * 100;
            Integer ceil = byTime.ceilingKey(nowSlot);
            return ceil != null ? ceil : byTime.lastKey();
        }
        if (byTime.containsKey(PREFERRED_HOUR_FUTURE)) {
            return PREFERRED_HOUR_FUTURE;
        }
        Integer ceil = byTime.ceilingKey(PREFERRED_HOUR_FUTURE);
        return ceil != null ? ceil : byTime.firstKey();
    }

    private List<HourlyWeather> buildHourly(TreeMap<Integer, Map<String, String>> byTime, int fromTime) {
        List<HourlyWeather> hourly = new ArrayList<>();
        for (Map.Entry<Integer, Map<String, String>> e : byTime.tailMap(fromTime, true).entrySet()) {
            Map<String, String> v = e.getValue();
            if (v.get("TMP") == null) {
                continue;
            }
            int hh = e.getKey() / 100;
            hourly.add(new HourlyWeather(
                    "%02d:00".formatted(hh),
                    roundSafeOr(v.get("TMP"), 0),
                    parseIntSafe(v.get("POP"), 0)
            ));
            if (hourly.size() >= MAX_HOURLY) {
                break;
            }
        }
        return hourly;
    }

    private int maxTemp(TreeMap<Integer, Map<String, String>> byTime, int fallback) {
        return byTime.values().stream()
                .map(v -> v.get("TMP"))
                .filter(java.util.Objects::nonNull)
                .map(this::roundSafeNullable)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(fallback);
    }

    private int minTemp(TreeMap<Integer, Map<String, String>> byTime, int fallback) {
        return byTime.values().stream()
                .map(v -> v.get("TMP"))
                .filter(java.util.Objects::nonNull)
                .map(this::roundSafeNullable)
                .filter(java.util.Objects::nonNull)
                .min(Integer::compareTo)
                .orElse(fallback);
    }

    private static int parseIntSafe(String s, int fallback) {
        if (s == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double parseDoubleSafe(String s, double fallback) {
        if (s == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int roundSafeOr(String s, int fallback) {
        Integer v = roundNullable(s);
        return v != null ? v : fallback;
    }

    private static Integer roundSafe(String s) {
        return roundNullable(s);
    }

    private Integer roundSafeNullable(String s) {
        return roundNullable(s);
    }

    private static Integer roundNullable(String s) {
        if (s == null) {
            return null;
        }
        try {
            return Math.round(Float.parseFloat(s.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
