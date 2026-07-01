package com.haeyaji.be.weather.domain;

import lombok.Builder;

import java.util.List;

/**
 * 날씨 도메인 모델. 외부 API(기상청 등)의 원시 응답을 fe 계약 형태로 축약한 결과.
 * 값 타입이므로 불변으로 다룬다.
 *
 * <p>단기예보(오늘~+2일)는 모든 필드를 채우지만, 중기예보(+3일~)는 시간별·습도·바람·체감이
 * 없으므로 {@code feels/humidity/windMs/hourly} 및 {@code uvIndex/pm} 는 null/빈값으로 내려간다.
 */
@Builder
public record Weather(
        WeatherCondition cond,
        String condKo,
        int temp,
        int hi,
        int lo,
        Integer feels,
        int pop,
        Integer humidity,
        Double windMs,
        Integer uvIndex,
        Integer pm10,
        Integer pm25,
        List<HourlyWeather> hourly
) {
    public Weather {
        hourly = hourly == null ? List.of() : List.copyOf(hourly);
    }

    /**
     * 자외선지수·미세먼지를 덧입힌 복사본을 만든다.
     * (UV/대기질은 별도 소스라 날씨 조회 후 보강한다. 실패 시 null 그대로.)
     */
    public Weather withAirQuality(Integer uvIndex, Integer pm10, Integer pm25) {
        return new Weather(cond, condKo, temp, hi, lo, feels, pop, humidity, windMs,
                uvIndex, pm10, pm25, hourly);
    }
}
