package com.haeyaji.be.recommend.api;

import com.haeyaji.be.recommend.api.dto.CoordinatesResponse;
import com.haeyaji.be.recommend.api.dto.PlacesResponse;
import com.haeyaji.be.recommend.application.port.out.PlaceProvider;
import com.haeyaji.be.recommend.application.port.out.PlaceSearchQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 장소 검색·지오코딩 프록시 (nlp 대행). 카카오 REST 키는 be가 보유한다.
 * 검색 "판단"(query_mapper: 카테고리코드·정렬·정규화)은 nlp가, "실행"(카카오 호출·정규화)은 be가 맡는다.
 *
 * <pre>
 * GET /api/places/search?query=&lat=&lng=&radiusM=&size=&sort=(accuracy|distance)&categoryGroupCode=
 * GET /api/places/geocode?query=
 * </pre>
 * (context-path 가 /api 이므로 매핑은 /places)
 */
@RestController
@RequestMapping("/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceProvider placeProvider;

    /** 실패·무결과도 200 + 빈 배열(nlp가 raise_for_status 로 폴백하지 않도록). */
    @GetMapping("/search")
    public PlacesResponse search(
            @RequestParam String query,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Integer radiusM,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "accuracy") String sort,
            @RequestParam(required = false) String categoryGroupCode
    ) {
        return PlacesResponse.from(placeProvider.search(
                new PlaceSearchQuery(query, lat, lng, radiusM, size, sort, categoryGroupCode)));
    }

    /** 200 {lat,lng} / 204 미발견(nlp는 None 처리 → 현재 위치 폴백). */
    @GetMapping("/geocode")
    public ResponseEntity<CoordinatesResponse> geocode(@RequestParam String query) {
        return placeProvider.geocode(query)
                .map(c -> ResponseEntity.ok(CoordinatesResponse.from(c)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
