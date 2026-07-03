package com.haeyaji.be.weather.application.port.out;

import com.haeyaji.be.weather.domain.LiveObservation;

/**
 * 아웃바운드 포트: 초단기실황(현재 관측값) 제공자.
 * 보강용이라 실패/미제공 시 null을 반환한다(날씨 본체를 깨지 않음).
 */
public interface NowcastProvider {

    LiveObservation getNowcast(double lat, double lng);
}
