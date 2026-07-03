package com.haeyaji.be.weather.domain;

/**
 * 초단기예보 시간별 한 칸 (향후 ~6시간, 매시 발표).
 *
 * @param time HH:mm
 * @param temp 기온 ℃ (T1H). 결측 시 null
 * @param sky  하늘상태 코드 (1 맑음, 3 구름많음, 4 흐림). 결측 시 null
 * @param pty  강수형태 코드. 결측 시 null
 */
public record UltraForecastSlot(String time, Integer temp, Integer sky, Integer pty) {
}
