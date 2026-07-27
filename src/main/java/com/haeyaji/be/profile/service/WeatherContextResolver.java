package com.haeyaji.be.profile.service;

import com.haeyaji.be.profile.domain.CtxWeather;
import com.haeyaji.be.weather.domain.Weather;
import com.haeyaji.be.weather.service.WeatherQuery;
import com.haeyaji.be.weather.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * 좌표를 학습 맥락의 날씨({@link CtxWeather})로 바꾼다 — 취향을 "비 오는 날/맑은 날"로 나눠 쌓기 위한 것.
 *
 * <p>학습(선택 피드백)과 조회(프로필 distill) 양쪽이 <b>같은 기준</b>으로 판정해야 쌓은 데이터를 되찾을 수 있어
 * 판정 로직을 한곳에 둔다.
 *
 * <p>날씨는 부가 맥락이므로 좌표가 없거나 상류 조회가 실패하면 {@link CtxWeather#CLEAR}로 폴백한다 —
 * 개인화 때문에 추천 자체가 실패하면 안 되기 때문. 조회 비용은 날씨 캐시(Redis)가 흡수한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherContextResolver {

    private final WeatherService weatherService;
    private final Clock clock;

    public CtxWeather resolve(Double lat, Double lng) {
        if (lat == null || lng == null) {
            return CtxWeather.CLEAR;
        }
        try {
            Weather weather = weatherService.getWeather(
                    new WeatherQuery(lat, lng, LocalDate.now(clock)));
            return CtxWeather.from(weather == null ? null : weather.cond());
        } catch (Exception e) {
            log.debug("날씨 맥락 조회 실패, CLEAR로 처리: lat={} lng={} err={}", lat, lng, e.toString());
            return CtxWeather.CLEAR;
        }
    }
}
