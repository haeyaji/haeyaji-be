package com.haeyaji.be.weather.client.airquality;

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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 미세먼지 아웃바운드 어댑터 (에어코리아, data.go.kr).
 *
 * <p><b>호출 전략 (일일 제한 500건 대응)</b>
 * <ul>
 *   <li>실측값: {@code getCtprvnRltmMesureDnsty?sidoName=전국} 1콜로 전국 측정소(~670개)를
 *       받아 60분 캐시 → 최대 24콜/일</li>
 *   <li>측정소 좌표: {@code getMsrstnList} 1콜을 24시간 캐시 → 1콜/일</li>
 * </ul>
 *
 * <p><b>값 선택</b>: 사용자 좌표에서 <b>최근접 측정소</b>의 실측값을 쓴다(동 단위 정밀도).
 * 최근접 측정소가 결측이면 다음 측정소로 넘어가고, 좌표 목록을 못 가져오면 시도 평균으로 폴백.
 *
 * <p>좌표 필드(dmX/dmY)는 제공처 버전에 따라 위도/경도 순서가 달라 값 범위로 자동 판별한다.
 * 보강용이므로 어떤 실패에도 예외를 던지지 않고 {@link AirQuality#EMPTY} 를 반환한다.
 */
@Slf4j
@Component
public class AirKoreaClient {

    /** 에어코리아 실시간 측정은 매시 갱신 → 전국 실측 60분 캐시. */
    private static final Duration MEASURE_TTL = Duration.ofMinutes(60);
    /** 측정소 위치는 거의 안 변함 → 24시간 캐시. */
    private static final Duration STATION_TTL = Duration.ofHours(24);
    private static final String SIDO_ALL = "전국";
    private static final int NUM_OF_ROWS = 1000;
    /** 최근접 탐색 시 결측 대비 후보 수. */
    private static final int NEAREST_CANDIDATES = 10;

    private final WebClient measureClient;
    private final WebClient stationClient;
    private final ObjectMapper objectMapper;
    private final String serviceKey;
    private final AtomicReference<MeasureCache> measureCache = new AtomicReference<>();
    private final AtomicReference<StationCache> stationCache = new AtomicReference<>();

    public AirKoreaClient(ObjectMapper objectMapper,
                          @Value("${haeyaji.weather.airquality.base-url}") String baseUrl,
                          @Value("${haeyaji.weather.airquality.station-base-url}") String stationBaseUrl,
                          @Value("${haeyaji.weather.datakr.service-key:}") String serviceKey) {
        // 전국 응답(~350KB)이 기본 버퍼(256KB)를 넘으므로 상향
        this.measureClient = WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
        this.stationClient = WebClient.builder()
                .baseUrl(stationBaseUrl)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
        this.objectMapper = objectMapper;
        this.serviceKey = serviceKey;
    }

    public AirQuality getAirQuality(double lat, double lng) {
        if (!StringUtils.hasText(serviceKey)) {
            return AirQuality.EMPTY;
        }
        JsonNode measures = nationwideMeasures();
        if (measures == null) {
            return AirQuality.EMPTY;
        }

        List<Station> stations = stationsWithCoords();
        if (!stations.isEmpty()) {
            AirQuality nearest = fromNearestStation(measures, stations, lat, lng);
            if (nearest != null) {
                return nearest;
            }
        }
        // 측정소 좌표 미확보/전체 결측 → 시도 평균 폴백
        SidoRegion region = SidoRegion.nearest(lat, lng);
        return new AirQuality(
                sidoAverage(measures, region.sidoName(), "pm10Value"),
                sidoAverage(measures, region.sidoName(), "pm25Value"));
    }

    /** 최근접 측정소부터 훑으며 pm10/pm25 각각 첫 유효값을 채운다. 둘 다 못 채우면 null. */
    private AirQuality fromNearestStation(JsonNode measures, List<Station> stations,
                                          double lat, double lng) {
        List<Station> candidates = stations.stream()
                .sorted(Comparator.comparingDouble(s -> squaredDistance(lat, lng, s.lat(), s.lng())))
                .limit(NEAREST_CANDIDATES)
                .toList();

        Integer pm10 = null;
        Integer pm25 = null;
        for (Station station : candidates) {
            JsonNode m = findMeasure(measures, station.name());
            if (m == null) {
                continue;
            }
            if (pm10 == null) {
                pm10 = parseValue(m.get("pm10Value"));
            }
            if (pm25 == null) {
                pm25 = parseValue(m.get("pm25Value"));
            }
            if (pm10 != null && pm25 != null) {
                break;
            }
        }
        return pm10 == null && pm25 == null ? null : new AirQuality(pm10, pm25);
    }

    private JsonNode findMeasure(JsonNode measures, String stationName) {
        for (JsonNode m : measures) {
            if (stationName.equals(m.path("stationName").asString(""))) {
                return m;
            }
        }
        return null;
    }

    // ---------- 전국 실측 캐시 ----------

    private JsonNode nationwideMeasures() {
        MeasureCache cached = measureCache.get();
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.items();
        }
        JsonNode fetched = fetchJsonItems(measureClient, "/getCtprvnRltmMesureDnsty", uri -> uri
                .queryParam("returnType", "json")
                .queryParam("numOfRows", NUM_OF_ROWS)
                .queryParam("pageNo", 1)
                .queryParam("sidoName", SIDO_ALL)
                .queryParam("ver", "1.5"));
        if (fetched != null) {
            measureCache.set(new MeasureCache(fetched, Instant.now().plus(MEASURE_TTL)));
        }
        return fetched;
    }

    // ---------- 측정소 좌표 캐시 ----------

    private List<Station> stationsWithCoords() {
        StationCache cached = stationCache.get();
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.stations();
        }
        JsonNode items = fetchJsonItems(stationClient, "/getMsrstnList", uri -> uri
                .queryParam("returnType", "json")
                .queryParam("numOfRows", NUM_OF_ROWS)
                .queryParam("pageNo", 1)
                .queryParam("ver", "1.3"));
        if (items == null) {
            return List.of(); // 미신청/실패 → 시도 평균 폴백 (캐시하지 않고 다음에 재시도)
        }
        List<Station> stations = parseStations(items);
        stationCache.set(new StationCache(stations, Instant.now().plus(STATION_TTL)));
        log.info("air quality stations loaded: {} (with coords)", stations.size());
        return stations;
    }

    /** dmX/dmY의 위도/경도 순서를 값 범위(한국: 위도 33~39, 경도 124~132)로 판별해 파싱. */
    private List<Station> parseStations(JsonNode items) {
        List<Station> stations = new ArrayList<>();
        for (JsonNode it : items) {
            String name = it.path("stationName").asString("");
            Double x = parseDouble(it.get("dmX"));
            Double y = parseDouble(it.get("dmY"));
            if (name.isEmpty() || x == null || y == null) {
                continue;
            }
            if (isLat(x) && isLng(y)) {
                stations.add(new Station(name, x, y));
            } else if (isLat(y) && isLng(x)) {
                stations.add(new Station(name, y, x));
            }
            // 둘 다 아니면(0 등 이상값) 스킵
        }
        return stations;
    }

    private static boolean isLat(double v) {
        return v >= 33.0 && v <= 39.5;
    }

    private static boolean isLng(double v) {
        return v >= 124.0 && v <= 132.5;
    }

    // ---------- 공통 ----------

    private JsonNode fetchJsonItems(WebClient client, String path,
                                    java.util.function.Function<org.springframework.web.util.UriBuilder,
                                            org.springframework.web.util.UriBuilder> params) {
        try {
            String body = client.get()
                    .uri(uri -> params.apply(uri.path(path).queryParam("serviceKey", serviceKey)).build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(8))
                    .block();
            JsonNode items = objectMapper.readTree(body).path("response").path("body").path("items");
            return items.isArray() ? items : null;
        } catch (Exception e) {
            log.warn("air quality fetch failed ({}): {}", path, e.toString());
            return null;
        }
    }

    /** 해당 시도 측정소들의 유효 수치 평균(반올림). 유효값 없으면 null. */
    private Integer sidoAverage(JsonNode measures, String sidoName, String field) {
        long sum = 0;
        int count = 0;
        for (JsonNode station : measures) {
            if (!matchesSido(station.path("sidoName").asString(""), sidoName)) {
                continue;
            }
            Integer v = parseValue(station.get(field));
            if (v != null) {
                sum += v;
                count++;
            }
        }
        return count == 0 ? null : Math.round((float) sum / count);
    }

    /** 전국 응답은 "전남광주"처럼 합쳐진 시도명이 있어 부분일치로 매칭. */
    private boolean matchesSido(String stationSido, String targetSido) {
        return !stationSido.isEmpty()
                && (stationSido.contains(targetSido) || targetSido.contains(stationSido));
    }

    private static Integer parseValue(JsonNode v) {
        if (v == null || v.isNull()) {
            return null;
        }
        String s = v.asString().trim();
        if (s.isEmpty() || "-".equals(s)) {
            return null; // 결측("-", 통신장애 등)
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double parseDouble(JsonNode v) {
        if (v == null || v.isNull()) {
            return null;
        }
        try {
            return Double.parseDouble(v.asString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static double squaredDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = lat1 - lat2;
        double dLng = (lng1 - lng2) * 0.8; // 한국 위도대 경도 보정
        return dLat * dLat + dLng * dLng;
    }

    private record Station(String name, double lat, double lng) {
    }

    private record MeasureCache(JsonNode items, Instant expiresAt) {
    }

    private record StationCache(List<Station> stations, Instant expiresAt) {
    }
}
