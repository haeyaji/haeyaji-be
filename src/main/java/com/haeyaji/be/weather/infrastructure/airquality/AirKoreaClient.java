package com.haeyaji.be.weather.infrastructure.airquality;

import com.haeyaji.be.weather.application.port.out.AirQualityProvider;
import com.haeyaji.be.weather.domain.AirQuality;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 미세먼지 아웃바운드 어댑터 (에어코리아 시도별 실시간 getCtprvnRltmMesureDnsty, data.go.kr).
 * 해당 시도 측정소들의 유효 pm10/pm25 평균을 낸다(도시 단위 대표값).
 *
 * <p>data.go.kr는 일일 트래픽 제한이 작으므로(개발계정 수백 건 수준) <b>시도 단위로 캐싱</b>한다.
 * 측정값이 1시간 주기로 갱신되므로 TTL 60분 → 상위 호출은 최대 17개 시도 × 24회/일로 바운드.
 *
 * <p>보강용이므로 어떤 실패에도 예외를 던지지 않고 {@link AirQuality#EMPTY} 를 반환한다.
 * 에어코리아는 apihub가 아니라 data.go.kr 소속이라 별도 serviceKey를 쓴다.
 */
@Slf4j
@Component
public class AirKoreaClient implements AirQualityProvider {

    /** 에어코리아 실시간 측정은 매시 갱신 → 시도별 60분 캐시. */
    private static final Duration CACHE_TTL = Duration.ofMinutes(60);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String serviceKey;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public AirKoreaClient(ObjectMapper objectMapper,
                          @Value("${haeyaji.weather.airquality.base-url}") String baseUrl,
                          @Value("${haeyaji.weather.datakr.service-key:}") String serviceKey) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.serviceKey = serviceKey;
    }

    @Override
    public AirQuality getAirQuality(double lat, double lng) {
        if (!StringUtils.hasText(serviceKey)) {
            return AirQuality.EMPTY;
        }
        SidoRegion region = SidoRegion.nearest(lat, lng);

        // 같은 시도는 좌표가 달라도 같은 값 → 시도 키로 캐싱해 일일 호출 제한을 지킨다.
        CacheEntry cached = cache.get(region.sidoName());
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.value();
        }

        AirQuality fetched = fetch(region, lat, lng);
        if (fetched != AirQuality.EMPTY) {
            cache.put(region.sidoName(), new CacheEntry(fetched, Instant.now().plus(CACHE_TTL)));
        }
        return fetched;
    }

    private AirQuality fetch(SidoRegion region, double lat, double lng) {
        try {
            String body = webClient.get()
                    .uri(uri -> uri.path("/getCtprvnRltmMesureDnsty")
                            .queryParam("serviceKey", serviceKey)
                            .queryParam("returnType", "json")
                            .queryParam("numOfRows", 100)
                            .queryParam("pageNo", 1)
                            .queryParam("sidoName", region.sidoName())
                            .queryParam("ver", "1.5")
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(8))
                    .block();

            JsonNode items = objectMapper.readTree(body).path("response").path("body").path("items");
            return new AirQuality(average(items, "pm10Value"), average(items, "pm25Value"));
        } catch (Exception e) {
            log.warn("air quality fetch failed (lat={}, lng={}): {}", lat, lng, e.toString());
            return AirQuality.EMPTY;
        }
    }

    /** 측정소 목록에서 유효 수치의 평균(반올림). 유효값 없으면 null. */
    private Integer average(JsonNode items, String field) {
        if (!items.isArray()) {
            return null;
        }
        long sum = 0;
        int count = 0;
        for (JsonNode station : items) {
            JsonNode v = station.get(field);
            if (v == null || v.isNull()) {
                continue;
            }
            String s = v.asString().trim();
            if (s.isEmpty() || "-".equals(s)) {
                continue;
            }
            try {
                sum += Integer.parseInt(s);
                count++;
            } catch (NumberFormatException ignored) {
                // 결측("-") 등 스킵
            }
        }
        return count == 0 ? null : Math.round((float) sum / count);
    }

    private record CacheEntry(AirQuality value, Instant expiresAt) {
    }
}
