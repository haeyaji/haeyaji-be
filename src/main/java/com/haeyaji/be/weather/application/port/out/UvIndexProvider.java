package com.haeyaji.be.weather.application.port.out;

import java.time.LocalDate;

/**
 * 아웃바운드 포트: 자외선지수 제공자(기상청 생활기상지수).
 * 보강용이라 실패/미제공 시 null을 반환한다(날씨 본체를 깨지 않음).
 */
public interface UvIndexProvider {

    /**
     * 대상 날짜의 대표(피크) 자외선지수. 없으면 null.
     */
    Integer getUvIndex(double lat, double lng, LocalDate date);
}
