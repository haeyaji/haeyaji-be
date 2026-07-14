package com.haeyaji.be.weather.client.kma;

import com.haeyaji.be.weather.domain.UltraForecastSlot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 초단기예보(getUltraSrtFcst) 아웃바운드 어댑터 — 향후 ~6시간, 매시 30분 발표.
 * 단기예보(3시간 주기)보다 돌발 강수(소나기)를 빠르게 반영한다.
 * 격자셀+발표시각 키로 캐싱한다(발표당 격자셀별 1콜).
 *
 * <p>보강용이므로 어떤 실패에도 예외를 던지지 않고 빈 목록을 반환한다(단기예보 값 유지).
 */
@Slf4j
@Component
public class KmaUltraForecastClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String authKey;
    /** nx:ny|발표시각 → 시간별 슬롯. 발표시각이 키에 포함되어 매시 자연 무효화. */
    private final Map<String, List<UltraForecastSlot>> cache = new ConcurrentHashMap<>();

    public KmaUltraForecastClient(WebClient.Builder webClientBuilder,
                                  ObjectMapper objectMapper,
                                  @Value("${haeyaji.weather.kma.base-url}") String baseUrl,
                                  @Value("${haeyaji.weather.kma.auth-key:}") String authKey) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.authKey = authKey;
    }

    public List<UltraForecastSlot> getUltraForecast(double lat, double lng) {
        if (!StringUtils.hasText(authKey)) {
            return List.of();
        }
        try {
            GridConverter.Grid grid = GridConverter.toGrid(lat, lng);
            KmaUltraBaseTime.BaseTime base = KmaUltraBaseTime.forecast(LocalDateTime.now());

            String key = grid.nx() + ":" + grid.ny() + "|" + base.baseDate() + base.baseTime();
            List<UltraForecastSlot> cached = cache.get(key);
            if (cached != null) {
                return cached;
            }

            List<UltraForecastSlot> fetched = fetch(grid, base);
            if (!fetched.isEmpty()) {
                if (cache.size() > 256) {
                    cache.clear(); // 지난 발표시각 키 정리
                }
                cache.put(key, fetched);
            }
            return fetched;
        } catch (Exception e) {
            log.warn("ultra forecast fetch failed (lat={}, lng={}): {}", lat, lng, e.toString());
            return List.of();
        }
    }

    private List<UltraForecastSlot> fetch(GridConverter.Grid grid, KmaUltraBaseTime.BaseTime base) {
        String body = webClient.get()
                .uri(uri -> uri.path("/getUltraSrtFcst")
                        .queryParam("authKey", authKey)
                        .queryParam("dataType", "JSON")
                        .queryParam("numOfRows", 100)
                        .queryParam("pageNo", 1)
                        .queryParam("base_date", base.baseDate())
                        .queryParam("base_time", base.baseTime())
                        .queryParam("nx", grid.nx())
                        .queryParam("ny", grid.ny())
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .block();

        JsonNode items = objectMapper.readTree(body)
                .path("response").path("body").path("items").path("item");
        if (!items.isArray() || items.isEmpty()) {
            return List.of();
        }

        // fcstDate+fcstTime → category → value (날짜 경계(자정) 넘어가는 슬롯 포함)
        TreeMap<String, Map<String, String>> bySlot = new TreeMap<>();
        for (JsonNode item : items) {
            String slot = item.path("fcstDate").asString("") + item.path("fcstTime").asString("");
            if (slot.length() != 12) {
                continue;
            }
            bySlot.computeIfAbsent(slot, k -> new java.util.HashMap<>())
                    .put(item.path("category").asString(""), item.path("fcstValue").asString(""));
        }

        List<UltraForecastSlot> slots = new ArrayList<>();
        for (Map.Entry<String, Map<String, String>> e : bySlot.entrySet()) {
            String hhmm = e.getKey().substring(8, 10) + ":" + e.getKey().substring(10, 12);
            Map<String, String> v = e.getValue();
            slots.add(new UltraForecastSlot(
                    hhmm,
                    parseInt(v.get("T1H")),
                    parseInt(v.get("SKY")),
                    parseInt(v.get("PTY"))
            ));
        }
        return List.copyOf(slots);
    }

    private static Integer parseInt(String s) {
        if (s == null) {
            return null;
        }
        try {
            return Math.round(Float.parseFloat(s.trim()));
        } catch (RuntimeException e) {
            return null;
        }
    }
}
