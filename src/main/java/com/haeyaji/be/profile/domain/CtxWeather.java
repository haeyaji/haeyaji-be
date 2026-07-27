package com.haeyaji.be.profile.domain;

import com.haeyaji.be.weather.domain.WeatherCondition;

/**
 * 가중치 학습 맥락 — 날씨. DDL {@code member_category_weight.ctx_weather} ENUM과 1:1.
 *
 * <p>같은 사람도 <b>비 오는 날과 맑은 날의 취향이 다르다</b>(비 오면 실내·카페, 맑으면 산책·야외).
 * 그래서 시간대와 함께 날씨를 맥락 축으로 두고 가중치를 나눠 쌓는다.
 *
 * <p>강수(비·눈)는 활동 선택을 크게 바꾸므로 {@link #RAINY}로 묶고, 나머지(맑음·흐림)는
 * {@link #CLEAR}로 본다 — 흐림만으로 실내를 택하진 않기 때문.
 */
public enum CtxWeather {
    RAINY,
    CLEAR;

    /** 날씨를 학습 맥락 두 갈래로 축약한다. 날씨를 못 받았으면(상류 실패 등) 기본값 {@link #CLEAR}. */
    public static CtxWeather from(WeatherCondition condition) {
        if (condition == null) {
            return CLEAR;
        }
        return switch (condition) {
            case RAINY, SNOWY -> RAINY;
            case SUNNY, CLOUDY -> CLEAR;
        };
    }
}
