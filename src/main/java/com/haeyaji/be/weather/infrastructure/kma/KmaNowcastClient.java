package com.haeyaji.be.weather.infrastructure.kma;

import com.haeyaji.be.weather.application.port.out.NowcastProvider;
import com.haeyaji.be.weather.domain.LiveObservation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 초단기실황(getUltraSrtNcst) 아웃바운드 어댑터 — 현재 시각의 <b>실제 관측값</b>.
 * 매시 정시 갱신되므로 격자셀+발표시각 키로 캐싱한다(발표당 격자셀별 1콜).
 *
 * <p>보강용이므로 어떤 실패에도 예외를 던지지 않고 null을 반환한다(단기예보 값 유지).
 */
@Slf4j
@Component
public class KmaNowcastClient implements NowcastProvider {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String authKey;
    /** nx:ny|발표시각 → 관측 스냅샷. 발표시각이 키에 포함되어 매시 자연 무효화. */
    private final Map<String, LiveObservation> cache = new ConcurrentHashMap<>();

    public KmaNowcastClient(WebClient.Builder webClientBuilder,
                            ObjectMapper objectMapper,
                            @Value("${haeyaji.weather.kma.base-url}") String baseUrl,
                            @Value("${haeyaji.weather.kma.auth-key:}") String authKey) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.authKey = authKey;
    }

    @Override
    public LiveObservation getNowcast(double lat, double lng) {
        if (!StringUtils.hasText(authKey)) {
            return null;
        }
        try {
            GridConverter.Grid grid = GridConverter.toGrid(lat, lng);
            KmaUltraBaseTime.BaseTime base = KmaUltraBaseTime.nowcast(LocalDateTime.now());

            String key = grid.nx() + ":" + grid.ny() + "|" + base.baseDate() + base.baseTime();
            LiveObservation cached = cache.get(key);
            if (cached != null) {
                return cached;
            }

            LiveObservation fetched = fetch(grid, base);
            if (fetched != null) {
                if (cache.size() > 256) {
                    cache.clear(); // 지난 발표시각 키 정리
                }
                cache.put(key, fetched);
            }
            return fetched;
        } catch (Exception e) {
            log.warn("nowcast fetch failed (lat={}, lng={}): {}", lat, lng, e.toString());
            return null;
        }
    }

    private LiveObservation fetch(GridConverter.Grid grid, KmaUltraBaseTime.BaseTime base) {
        String body = webClient.get()
                .uri(uri -> uri.path("/getUltraSrtNcst")
                        .queryParam("authKey", authKey)
                        .queryParam("dataType", "JSON")
                        .queryParam("numOfRows", 20)
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
            return null;
        }

        Double t1h = null;
        Integer reh = null;
        Double wsd = null;
        int pty = 0;
        for (JsonNode item : items) {
            String category = item.path("category").asString("");
            String value = item.path("obsrValue").asString("");
            switch (category) {
                case "T1H" -> t1h = parseDouble(value);
                case "REH" -> reh = parseInt(value);
                case "WSD" -> wsd = parseDouble(value);
                case "PTY" -> pty = parseInt(value) != null ? parseInt(value) : 0;
                default -> { /* RN1·UUU 등 미사용 */ }
            }
        }
        if (t1h == null) {
            return null; // 기온 실측 없으면 실황 무의미
        }
        return new LiveObservation(t1h, reh, wsd, pty);
    }

    private static Double parseDouble(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Integer parseInt(String s) {
        try {
            return Math.round(Float.parseFloat(s.trim()));
        } catch (RuntimeException e) {
            return null;
        }
    }
}
