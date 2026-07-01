package com.haeyaji.be.weather.domain;

/**
 * 대기질 값(미세먼지). 값이 없으면 null.
 *
 * @param pm10 미세먼지 ㎍/㎥
 * @param pm25 초미세먼지 ㎍/㎥
 */
public record AirQuality(Integer pm10, Integer pm25) {

    public static final AirQuality EMPTY = new AirQuality(null, null);
}
