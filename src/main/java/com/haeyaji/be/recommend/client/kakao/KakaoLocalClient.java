package com.haeyaji.be.recommend.client.kakao;

import com.haeyaji.be.recommend.service.PlaceSearchQuery;
import com.haeyaji.be.recommend.domain.Coordinates;
import com.haeyaji.be.recommend.domain.Place;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * 카카오 로컬 API 아웃바운드 어댑터. 키워드 검색·지오코딩을 대행하고 응답을 {@link Place} 로 정규화한다.
 *
 * <p>키(REST 키)는 be가 보유하고 {@code Authorization: KakaoAK {key}} 헤더로 인증한다.
 * nlp 대행 프록시라 어떤 실패(키 미설정·타임아웃·오류 응답)에도 예외를 던지지 않고
 * 검색은 빈 리스트, 지오코딩은 {@link Optional#empty()} 로 흡수한다.
 *
 * <p>지오코딩도 키워드 검색으로 구현한다: "강남역"·"성수동" 같은 지역·POI명을 주소검색보다 잘 잡기 때문.
 */
@Slf4j
@Component
public class KakaoLocalClient {

    private static final String KEYWORD_PATH = "/v2/local/search/keyword.json";
    private static final int MIN_SIZE = 1;
    private static final int PAGE_SIZE = 15;     // 카카오 keyword 페이지당 상한
    private static final int MAX_PAGES = 3;      // 카카오가 페이징으로 내주는 최대 45건 = 15 x 3
    private static final int MAX_TOTAL = PAGE_SIZE * MAX_PAGES;
    private static final int MAX_RADIUS_M = 20000; // 카카오 radius 상한(m)
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String restKey;

    @Autowired
    public KakaoLocalClient(ObjectMapper objectMapper,
                            @Value("${haeyaji.kakao.base-url}") String baseUrl,
                            @Value("${haeyaji.kakao.rest-key:}") String restKey) {
        this(objectMapper, WebClient.builder().baseUrl(baseUrl).build(), restKey);
    }

    /** 테스트용: WebClient(ExchangeFunction 스텁)를 직접 주입해 HTTP 없이 매핑 검증. */
    KakaoLocalClient(ObjectMapper objectMapper, WebClient webClient, String restKey) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.restKey = restKey;
    }

    /**
     * 키워드 검색. {@code size}를 지정하면 그 개수까지만, <b>지정하지 않으면 반경 내 전부</b>를 모은다.
     * <p>카카오는 한 번에 최대 15건만 주므로 필요한 만큼 페이지를 넘겨 모은다(제공 상한 45건).
     * 페이지 경계가 어긋나지 않도록 요청 크기는 항상 15로 고정하고, 목표 개수만큼만 잘라서 반환한다.
     * 지도 탐색은 "반경 안의 모든 후보"가 필요해 전부 받고, nlp 추천처럼 소수만 필요하면 size를 준다.
     */
    public List<Place> search(PlaceSearchQuery q) {
        int target = q.size() != null ? clamp(q.size(), MIN_SIZE, MAX_TOTAL) : MAX_TOTAL;
        List<Place> places = new ArrayList<>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            final int pageNo = page;
            JsonNode docs = call(uri -> buildSearchUri(uri, q, pageNo));
            if (docs == null || docs.isEmpty()) {
                break;
            }
            for (JsonNode d : docs) {
                places.add(toPlace(d));
            }
            if (docs.size() < PAGE_SIZE || places.size() >= target) {
                break; // 마지막 페이지이거나 목표치를 채움
            }
        }
        return places.size() > target ? List.copyOf(places.subList(0, target)) : places;
    }

    private static URI buildSearchUri(UriBuilder uri, PlaceSearchQuery q, int page) {
        UriBuilder b = uri.path(KEYWORD_PATH)
                .queryParam("query", q.query())
                .queryParam("size", PAGE_SIZE)
                .queryParam("page", page);
        boolean hasCenter = q.lat() != null && q.lng() != null;
        if (hasCenter) {
            b.queryParam("x", q.lng()).queryParam("y", q.lat()); // x=경도, y=위도
            if (q.radiusM() != null) {
                b.queryParam("radius", clamp(q.radiusM(), 0, MAX_RADIUS_M));
            }
        }
        // distance 정렬은 중심 좌표가 있어야 유효 → 없으면 accuracy 로 강등
        String sort = "distance".equals(q.sort()) && hasCenter ? "distance" : "accuracy";
        b.queryParam("sort", sort);
        if (StringUtils.hasText(q.categoryGroupCode())) {
            b.queryParam("category_group_code", q.categoryGroupCode());
        }
        return b.build();
    }

    public Optional<Coordinates> geocode(String query) {
        JsonNode docs = call(uri -> uri.path(KEYWORD_PATH)
                .queryParam("query", query)
                .queryParam("size", 1)
                .build());
        if (docs == null || docs.isEmpty()) {
            return Optional.empty();
        }
        JsonNode top = docs.get(0);
        Double x = parseDouble(top.get("x")); // 경도
        Double y = parseDouble(top.get("y")); // 위도
        if (x == null || y == null) {
            return Optional.empty();
        }
        return Optional.of(new Coordinates(y, x));
    }

    /** documents 배열 노드 반환. 키 미설정·오류·타임아웃 시 null. */
    private JsonNode call(Function<UriBuilder, URI> uriFn) {
        if (!StringUtils.hasText(restKey)) {
            log.error("Kakao REST key is not configured (KAKAO_REST_KEY)");
            return null;
        }
        try {
            String body = webClient.get()
                    .uri(uriFn)
                    .header("Authorization", "KakaoAK " + restKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(TIMEOUT)
                    .block();
            JsonNode docs = objectMapper.readTree(body).path("documents");
            return docs.isArray() ? docs : null;
        } catch (Exception e) {
            log.warn("kakao local call failed: {}", e.toString());
            return null;
        }
    }

    private static Place toPlace(JsonNode d) {
        String category = text(d, "category_group_name");
        if (!StringUtils.hasText(category)) {
            category = text(d, "category_name"); // 그룹명이 없으면 전체 분류 경로로 폴백
        }
        String address = text(d, "road_address_name");
        if (!StringUtils.hasText(address)) {
            address = text(d, "address_name"); // 도로명이 없으면 지번주소로 폴백
        }
        return new Place(
                text(d, "place_name"),
                category,
                address,
                text(d, "place_url"),
                parseInt(d.get("distance")), // 중심 좌표+정렬 있을 때만 제공 → nullable
                parseDouble(d.get("x")),     // 경도
                parseDouble(d.get("y"))      // 위도
        );
    }

    private static int clamp(int v, int min, int max) {
        return Math.min(Math.max(v, min), max);
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? "" : v.asString();
    }

    private static Integer parseInt(JsonNode v) {
        if (v == null || v.isNull()) {
            return null;
        }
        String s = v.asString().trim();
        if (s.isEmpty()) {
            return null;
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
}
