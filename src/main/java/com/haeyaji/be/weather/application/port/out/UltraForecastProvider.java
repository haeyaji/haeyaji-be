package com.haeyaji.be.weather.application.port.out;

import com.haeyaji.be.weather.domain.UltraForecastSlot;

import java.util.List;

/**
 * 아웃바운드 포트: 초단기예보(향후 ~6시간, 매시 갱신) 제공자.
 * 보강용이라 실패/미제공 시 빈 목록을 반환한다(날씨 본체를 깨지 않음).
 */
public interface UltraForecastProvider {

    List<UltraForecastSlot> getUltraForecast(double lat, double lng);
}
