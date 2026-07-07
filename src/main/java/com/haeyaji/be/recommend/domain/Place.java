package com.haeyaji.be.recommend.domain;

/**
 * 카카오 로컬에서 가져온 장소. nlp Place 계약과 1:1 (name·category·address·url·distanceM·x·y).
 * <p>distanceM/x/y 는 검색 파라미터·응답에 따라 결측 가능하므로 nullable. x=경도, y=위도.
 */
public record Place(
        String name,
        String category,
        String address,
        String url,
        Integer distanceM,
        Double x,
        Double y
) {
}
