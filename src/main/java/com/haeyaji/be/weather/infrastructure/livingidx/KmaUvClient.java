package com.haeyaji.be.weather.infrastructure.livingidx;

import com.haeyaji.be.weather.application.port.out.UvIndexProvider;
import com.haeyaji.be.weather.infrastructure.airquality.SidoRegion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 자외선지수 아웃바운드 어댑터 (기상청 생활기상지수 getUVIdxV5 = 조회서비스 3.0, data.go.kr).
 * 3시간 간격 h0~h75 예측값 중 대상 날짜의 <b>피크(최댓값)</b>를 대표 UV로 쓴다.
 *
 * <p>data.go.kr serviceKey는 에어코리아(미세먼지)와 동일 키를 공유한다(계정당 1키).
 * 보강용이므로 어떤 실패에도 예외를 던지지 않고 null을 반환한다.
 */
@Slf4j
@Component
public class KmaUvClient implements UvIndexProvider {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyyMMddHH");
    private static final int MAX_H = 75;
    // 발표 반영 버퍼 시각. 이 시각 전이면 직전 발표(06/18) 사용.
    private static final int AVAILABLE_AFTER_HOUR = 8;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String serviceKey;

    public KmaUvClient(ObjectMapper objectMapper,
                       @Value("${haeyaji.weather.uv.base-url}") String baseUrl,
                       @Value("${haeyaji.weather.datakr.service-key:}") String serviceKey) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.serviceKey = serviceKey;
    }

    @Override
    public Integer getUvIndex(double lat, double lng, LocalDate date) {
        if (!StringUtils.hasText(serviceKey)) {
            return null;
        }
        try {
            SidoRegion region = SidoRegion.nearest(lat, lng);
            LocalDateTime baseTime = resolveBaseTime(LocalDateTime.now());
            String time = baseTime.format(TIME);

            String body = webClient.get()
                    .uri(uri -> uri.path("/getUVIdxV5")
                            .queryParam("serviceKey", serviceKey)
                            .queryParam("dataType", "JSON")
                            .queryParam("numOfRows", 10)
                            .queryParam("pageNo", 1)
                            .queryParam("areaNo", region.areaNo())
                            .queryParam("time", time)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(8))
                    .block();

            JsonNode item = objectMapper.readTree(body)
                    .path("response").path("body").path("items").path("item");
            if (item.isArray() && !item.isEmpty()) {
                item = item.get(0);
            }
            return peakForDate(item, baseTime, date);
        } catch (Exception e) {
            log.warn("UV index fetch failed (lat={}, lng={}): {}", lat, lng, e.toString());
            return null;
        }
    }

    /** 대상 날짜에 해당하는 h-필드들 중 최댓값(피크 UV). 없으면 null. */
    private Integer peakForDate(JsonNode item, LocalDateTime baseTime, LocalDate date) {
        Integer peak = null;
        for (int h = 0; h <= MAX_H; h += 3) {
            if (!baseTime.plusHours(h).toLocalDate().equals(date)) {
                continue;
            }
            JsonNode v = item.get("h" + h);
            if (v == null || v.isNull() || v.asString().isBlank()) {
                continue;
            }
            try {
                int uv = Integer.parseInt(v.asString().trim());
                peak = peak == null ? uv : Math.max(peak, uv);
            } catch (NumberFormatException ignored) {
                // 야간 등 빈값/비수치 → 스킵
            }
        }
        return peak;
    }

    /** 생활기상지수 발표시각(06/18시). 버퍼 이전이면 직전 발표 사용. */
    private LocalDateTime resolveBaseTime(LocalDateTime now) {
        LocalDateTime t = now.withMinute(0).withSecond(0).withNano(0);
        if (now.getHour() >= 18 + (AVAILABLE_AFTER_HOUR - 6)) {
            return t.withHour(18);
        }
        if (now.getHour() >= AVAILABLE_AFTER_HOUR) {
            return t.withHour(6);
        }
        return t.minusDays(1).withHour(18);
    }
}
