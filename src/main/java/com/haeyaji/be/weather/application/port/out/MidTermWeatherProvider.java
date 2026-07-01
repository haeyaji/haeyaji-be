package com.haeyaji.be.weather.application.port.out;

import com.haeyaji.be.weather.domain.Weather;

import java.time.LocalDate;

/**
 * 아웃바운드 포트: 중기예보 제공자(기상청 중기육상예보 + 중기기온).
 * +4일~+10일 범위, 지역코드 기반 일 단위(오전/오후) 예보.
 *
 * <p>시간별·습도·바람·체감은 제공하지 않으므로 매핑 결과의 해당 필드는 null/빈값이다.
 */
public interface MidTermWeatherProvider {

    /**
     * @param lat  위도
     * @param lng  경도
     * @param date 조회 날짜 (중기예보 범위 내)
     */
    Weather fetch(double lat, double lng, LocalDate date);
}
