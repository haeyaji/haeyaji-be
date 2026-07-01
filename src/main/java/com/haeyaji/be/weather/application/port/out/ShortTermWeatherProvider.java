package com.haeyaji.be.weather.application.port.out;

import com.haeyaji.be.weather.domain.Weather;

import java.time.LocalDate;

/**
 * 아웃바운드 포트: 단기예보 제공자(기상청 동네예보). 오늘~+3일, 시간별 상세 제공.
 * 구현체는 좌표→격자 변환·API 호출·도메인 매핑을 책임진다.
 */
public interface ShortTermWeatherProvider {

    /**
     * @param lat  위도
     * @param lng  경도
     * @param date 조회 날짜 (단기예보 범위 내)
     */
    Weather fetch(double lat, double lng, LocalDate date);
}
