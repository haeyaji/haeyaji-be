package com.haeyaji.be.weather.domain;

/**
 * 시간별 예보 한 칸.
 *
 * @param time HH:mm
 * @param temp 기온(℃)
 * @param pop  강수확률(%)
 */
public record HourlyWeather(String time, int temp, int pop) {
}
