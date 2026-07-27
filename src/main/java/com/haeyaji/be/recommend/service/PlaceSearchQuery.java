package com.haeyaji.be.recommend.service;

/**
 * 장소 검색 파라미터. nlp(query_mapper)가 판단해 넘긴 값을 그대로 카카오로 전달한다.
 * <p>lat/lng 는 중심 좌표(옵션) — 없으면 반경·거리정렬 없이 키워드 검색만 수행한다.
 * <p>{@code size}가 null이면 <b>개수 제한 없이 반경 내 전부</b>를 가져온다(카카오 제공 상한까지).
 *    지도 탐색처럼 "반경 안의 모든 후보"가 필요할 때 쓰고, 추천처럼 소수만 필요하면 값을 지정한다.
 */
public record PlaceSearchQuery(
        String query,
        Double lat,
        Double lng,
        Integer radiusM,
        Integer size,
        String sort,
        String categoryGroupCode
) {
}
