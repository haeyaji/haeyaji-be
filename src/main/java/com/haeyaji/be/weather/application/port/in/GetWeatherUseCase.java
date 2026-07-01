package com.haeyaji.be.weather.application.port.in;

import com.haeyaji.be.weather.domain.Weather;

/**
 * 인바운드 포트: 좌표·날짜로 날씨를 조회한다.
 */
public interface GetWeatherUseCase {

    Weather getWeather(WeatherQuery query);
}
