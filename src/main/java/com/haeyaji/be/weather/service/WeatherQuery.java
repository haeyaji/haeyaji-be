package com.haeyaji.be.weather.service;

import java.time.LocalDate;

/**
 * 날씨 조회 요청. date는 선택(null 이면 오늘).
 *
 * @param lat  위도 (필수)
 * @param lng  경도 (필수)
 * @param date 조회 날짜 (선택)
 */
public record WeatherQuery(double lat, double lng, LocalDate date) {
}
