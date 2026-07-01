package com.haeyaji.be.weather.application.port.out;

import com.haeyaji.be.weather.domain.AirQuality;

/**
 * 아웃바운드 포트: 대기질(미세먼지) 제공자(에어코리아 실시간).
 * 실시간 관측이라 '현재' 기준이며, 보강용이라 실패 시 {@link AirQuality#EMPTY} 를 반환한다.
 */
public interface AirQualityProvider {

    AirQuality getAirQuality(double lat, double lng);
}
