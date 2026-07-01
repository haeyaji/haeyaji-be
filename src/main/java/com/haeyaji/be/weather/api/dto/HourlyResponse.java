package com.haeyaji.be.weather.api.dto;

import com.haeyaji.be.weather.domain.HourlyWeather;

/**
 * 시간별 예보 응답 항목 (fe 계약: time/temp/pop).
 */
public record HourlyResponse(String time, int temp, int pop) {

    public static HourlyResponse from(HourlyWeather h) {
        return new HourlyResponse(h.time(), h.temp(), h.pop());
    }
}
