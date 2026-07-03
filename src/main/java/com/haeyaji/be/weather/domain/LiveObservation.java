package com.haeyaji.be.weather.domain;

/**
 * 초단기실황 관측 스냅샷 (예보가 아닌 실측값).
 *
 * @param temp     기온 ℃ (T1H 실측, 소수 포함)
 * @param humidity 습도 % (REH). 결측 시 null
 * @param windMs   풍속 m/s (WSD). 결측 시 null
 * @param pty      강수형태 코드 (0 없음, 1 비, 2 비/눈, 3 눈, 5 빗방울, 6 빗방울눈날림, 7 눈날림)
 */
public record LiveObservation(double temp, Integer humidity, Double windMs, int pty) {
}
