package com.haeyaji.be.profile.domain;

import com.haeyaji.be.weather.domain.WeatherCondition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 날씨를 학습 맥락 두 갈래로 축약하는 규칙 — 학습과 조회가 같은 기준을 써야 쌓은 취향을 되찾는다.
 */
class CtxWeatherTest {

    @Test
    void 비와_눈은_실내_성향_맥락으로_묶인다() {
        assertThat(CtxWeather.from(WeatherCondition.RAINY)).isEqualTo(CtxWeather.RAINY);
        assertThat(CtxWeather.from(WeatherCondition.SNOWY)).isEqualTo(CtxWeather.RAINY);
    }

    @Test
    void 흐림은_맑음과_같이_본다() {
        // 흐리다고 실내를 택하진 않으므로 강수 여부만 가른다.
        assertThat(CtxWeather.from(WeatherCondition.CLOUDY)).isEqualTo(CtxWeather.CLEAR);
        assertThat(CtxWeather.from(WeatherCondition.SUNNY)).isEqualTo(CtxWeather.CLEAR);
    }

    @Test
    void 날씨를_못_받으면_맑음으로_간주한다() {
        // 상류 실패·좌표 없음 때문에 개인화가 멈추면 안 된다.
        assertThat(CtxWeather.from(null)).isEqualTo(CtxWeather.CLEAR);
    }
}
