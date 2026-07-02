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
import java.util.concurrent.atomic.AtomicReference;

/**
 * 미세먼지 아웃바운드 어댑터 (에어코리아 getCtprvnRltmMesureDnsty, data.go.kr).
 *
 * <p><b>전국 1콜 캐싱</b>: {@code sidoName=전국} 으로 전국 측정소(~670개)를 한 번에 받아
 * 60분 캐시하고, 요청 좌표의 시도에 해당하는 측정소들만 걸러 평균 낸다.
 * → 상위 호출은 지역·사용자 수와 무관하게 <b>최대 24건/일</b> (일일 제한 500건 대비 안전).
 *
 * <p>주의: 전국 조회 응답의 sidoName은 "전남광주"처럼 합쳐진 표기가 있어 contains 로 매칭한다.
 *
 * <p>보강용이므로 어떤 실패에도 예외를 던지지 않고 {@link AirQuality#EMPTY} 를 반환한다.
 * 에어코리아는 apihub가 아니라 data.go.kr 소속이라 별도 serviceKey를 쓴다.
 */
@Slf4j
@Component
public class AirKoreaClient implements AirQualityProvider {

    /** 에어코리아 실시간 측정은 매시 갱신 → 전국 응답 60분 캐시. */
    private static final Duration CACHE_TTL = Duration.ofMinutes(60);
    private static final String SIDO_ALL = "전국";
    /** 전국 측정소 ~670개를 한 페이지로 수용. */
    private static final int NUM_OF_ROWS = 1000;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String serviceKey;
    /** 전국 응답 캐시 (측정소 목록 + 만료시각). */
    private final AtomicReference<CacheEntry> cache = new AtomicReference<>();

    public AirKoreaClient(ObjectMapper objectMapper,
                          @Value("${haeyaji.weather.airquality.base-url}") String baseUrl,
                          @Value("${haeyaji.weather.datakr.service-key:}") String serviceKey) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                // 전국 응답(~350KB)이 기본 버퍼(256KB)를 넘으므로 상향
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
        this.objectMapper = objectMapper;
        this.serviceKey = serviceKey;
    }

    @Override
    public AirQuality getAirQuality(double lat, double lng) {
        if (!StringUtils.hasText(serviceKey)) {
            return AirQuality.EMPTY;
        }
        JsonNode stations = nationwideStations();
        if (stations == null) {
            return AirQuality.EMPTY;
        }
        SidoRegion region = SidoRegion.nearest(lat, lng);
        return new AirQuality(
                average(stations, region.sidoName(), "pm10Value"),
                average(stations, region.sidoName(), "pm25Value"));
    }

    /** 전국 측정소 목록(캐시 우선). 실패 시 null. */
    private JsonNode nationwideStations() {
        CacheEntry cached = cache.get();
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.stations();
        }
        JsonNode fetched = fetchNationwide();
        if (fetched != null) {
            cache.set(new CacheEntry(fetched, Instant.now().plus(CACHE_TTL)));
        }
        return fetched;
    }

    private JsonNode fetchNationwide() {
        try {
            String body = webClient.get()
                    .uri(uri -> uri.path("/getCtprvnRltmMesureDnsty")
                            .queryParam("serviceKey", serviceKey)
                            .queryParam("returnType", "json")
                            .queryParam("numOfRows", NUM_OF_ROWS)
                            .queryParam("pageNo", 1)
                            .queryParam("sidoName", SIDO_ALL)
                            .queryParam("ver", "1.5")
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(8))
                    .block();

            JsonNode items = objectMapper.readTree(body).path("response").path("body").path("items");
            return items.isArray() ? items : null;
        } catch (Exception e) {
            log.warn("air quality nationwide fetch failed: {}", e.toString());
            return null;
        }
    }

    /** 해당 시도 측정소들의 유효 수치 평균(반올림). 유효값 없으면 null. */
    private Integer average(JsonNode stations, String sidoName, String field) {
        long sum = 0;
        int count = 0;
        for (JsonNode station : stations) {
            if (!matchesSido(station.path("sidoName").asString(""), sidoName)) {
                continue;
            }
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

    /** 전국 응답은 "전남광주"처럼 합쳐진 시도명이 있어 부분일치로 매칭. */
    private boolean matchesSido(String stationSido, String targetSido) {
        return !stationSido.isEmpty()
                && (stationSido.contains(targetSido) || targetSido.contains(stationSido));
    }

    private record CacheEntry(JsonNode stations, Instant expiresAt) {
    }
}
