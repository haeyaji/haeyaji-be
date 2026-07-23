package com.haeyaji.be.profile.domain;

import java.util.Optional;

/**
 * 개인화 카테고리 10종(고정). 취향의 "거친 축" — 세부 취향은 {@code member_keyword_weight}(키워드 축)가 담당.
 * 값은 nlp·fe·be 공통 계약 코드값이며 DDL {@code member_category_weight.category} ENUM과 1:1.
 */
public enum Category {
    CAFE_DESSERT,     // 카페·디저트
    RESTAURANT,       // 맛집
    NATURE_WALK,      // 산책·자연
    SPORTS_ACTIVITY,  // 운동·액티비티
    CULTURE_EXHIBIT,  // 문화·전시
    INDOOR_PLAY,      // 실내놀이(방탈출·보드게임 등)
    REST_HEALING,     // 휴식·힐링
    STUDY_WORK,       // 공부·작업
    SOCIAL,           // 사람만남
    SHOPPING;         // 쇼핑

    /**
     * 자유문자열을 카테고리로 안전 파싱. 10종이 아니면 {@link Optional#empty()}.
     * 외부(nlp/fe) 입력을 신뢰하지 않고 이 가드로 유효 코드만 학습에 반영한다.
     */
    public static Optional<Category> tryParse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Category.valueOf(value.trim()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
