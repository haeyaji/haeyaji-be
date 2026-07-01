package com.haeyaji.be.weather.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherConditionTest {

    @Test
    void 강수가_있으면_비와_상관없이_rainy() {
        assertThat(WeatherCondition.resolve(1, 1)).isEqualTo(WeatherCondition.RAINY);
        assertThat(WeatherCondition.resolve(4, 3)).isEqualTo(WeatherCondition.RAINY);
    }

    @Test
    void 맑음은_sunny_구름많음_흐림은_cloudy() {
        assertThat(WeatherCondition.resolve(1, 0)).isEqualTo(WeatherCondition.SUNNY);
        assertThat(WeatherCondition.resolve(3, 0)).isEqualTo(WeatherCondition.CLOUDY);
        assertThat(WeatherCondition.resolve(4, 0)).isEqualTo(WeatherCondition.CLOUDY);
    }

    @Test
    void code는_소문자_계약값() {
        assertThat(WeatherCondition.SUNNY.code()).isEqualTo("sunny");
        assertThat(WeatherCondition.CLOUDY.code()).isEqualTo("cloudy");
        assertThat(WeatherCondition.RAINY.code()).isEqualTo("rainy");
    }

    @Test
    void condKo는_강수형태_하늘상태를_한글로() {
        assertThat(WeatherCondition.describeKo(1, 0)).isEqualTo("맑음");
        assertThat(WeatherCondition.describeKo(3, 0)).isEqualTo("구름많음");
        assertThat(WeatherCondition.describeKo(4, 0)).isEqualTo("흐림");
        assertThat(WeatherCondition.describeKo(1, 1)).isEqualTo("비");
        assertThat(WeatherCondition.describeKo(1, 4)).isEqualTo("소나기");
    }
}
