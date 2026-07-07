package com.haeyaji.be.recommend.application.port.out;

/**
 * 장소 검색 파라미터. nlp(query_mapper)가 판단해 넘긴 값을 그대로 카카오로 전달한다.
 * <p>lat/lng 는 중심 좌표(옵션) — 없으면 반경·거리정렬 없이 키워드 검색만 수행한다.
 */
public record PlaceSearchQuery(
        String query,
        Double lat,
        Double lng,
        Integer radiusM,
        int size,
        String sort,
        String categoryGroupCode
) {
}
