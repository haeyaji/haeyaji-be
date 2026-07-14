package com.haeyaji.be.weather.dto;

import com.haeyaji.be.weather.domain.Weather;

import java.util.List;

/**
 * GET /api/weather 응답 (camelCase — nlp 계약과 톤 일치).
 * uvIndex/pm10/pm25 는 미제공 시 null 로 내려간다(fe에서 게이지 파생).
 */
public record WeatherResponse(
        String cond,
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
        List<HourlyResponse> hourly
) {

    public static WeatherResponse from(Weather w) {
        return new WeatherResponse(
                w.cond().code(),
                w.condKo(),
                w.temp(),
                w.hi(),
                w.lo(),
                w.feels(),
                w.pop(),
                w.humidity(),
                w.windMs(),
                w.uvIndex(),
                w.pm10(),
                w.pm25(),
                w.hourly().stream().map(HourlyResponse::from).toList()
        );
    }
}
