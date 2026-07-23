package com.haeyaji.be.profile.domain;

/**
 * 가중치 학습 맥락 — 날씨. DDL {@code member_category_weight.ctx_weather} ENUM과 1:1.
 * <p>MVP: fe feedback 요청에 날씨가 없어 {@link #CLEAR}를 기본 맥락으로 쓴다.
 * 날씨별 세분화는 feedback DTO에 weather가 실리면(데이터 축적 후) 활성화한다.
 */
public enum CtxWeather {
    RAINY,
    CLEAR
}
