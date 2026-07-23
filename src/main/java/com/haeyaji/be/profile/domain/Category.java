package com.haeyaji.be.profile.domain;

/**
 * 개인화 카테고리 6종(고정). todo 테이블엔 category가 없고(선택 시점 학습 결정),
 * 취향은 이 6종 가중치로만 관리한다. 값은 fe feedback·distill 계약과 동일한 코드값.
 * DDL {@code member_category_weight.category} ENUM과 1:1.
 */
public enum Category {
    OUTDOOR,
    INDOOR,
    REST,
    PRODUCTIVITY,
    MEETING_PEOPLE,
    FOOD_CAFE
}
