package com.haeyaji.be.weather.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherConditionTest {

    @Test
    void 비_소나기는_rainy() {
        assertThat(WeatherCondition.resolve(1, 1, 10)).isEqualTo(WeatherCondition.RAINY);
        assertThat(WeatherCondition.resolve(4, 4, 20)).isEqualTo(WeatherCondition.RAINY);
        assertThat(WeatherCondition.resolve(4, 5, 15)).isEqualTo(WeatherCondition.RAINY);
    }

    @Test
    void 눈_눈날림은_snowy() {
        assertThat(WeatherCondition.resolve(4, 3, -2)).isEqualTo(WeatherCondition.SNOWY);
        assertThat(WeatherCondition.resolve(4, 7, 0)).isEqualTo(WeatherCondition.SNOWY);
    }

    @Test
    void 비눈_혼재는_기온으로_판정() {
        // PTY 2 (비/눈): 1℃ 이하 → snowy, 초과 → rainy
        assertThat(WeatherCondition.resolve(4, 2, 0)).isEqualTo(WeatherCondition.SNOWY);
        assertThat(WeatherCondition.resolve(4, 2, 1)).isEqualTo(WeatherCondition.SNOWY);
        assertThat(WeatherCondition.resolve(4, 2, 2)).isEqualTo(WeatherCondition.RAINY);
        // PTY 6 (빗방울눈날림) 동일 정책
        assertThat(WeatherCondition.resolve(4, 6, -1)).isEqualTo(WeatherCondition.SNOWY);
        assertThat(WeatherCondition.resolve(4, 6, 5)).isEqualTo(WeatherCondition.RAINY);
    }

    @Test
    void 맑음은_sunny_구름많음_흐림은_cloudy() {
        assertThat(WeatherCondition.resolve(1, 0, 20)).isEqualTo(WeatherCondition.SUNNY);
        assertThat(WeatherCondition.resolve(3, 0, 20)).isEqualTo(WeatherCondition.CLOUDY);
        assertThat(WeatherCondition.resolve(4, 0, 20)).isEqualTo(WeatherCondition.CLOUDY);
    }

    @Test
    void code는_소문자_계약값() {
        assertThat(WeatherCondition.SUNNY.code()).isEqualTo("sunny");
        assertThat(WeatherCondition.CLOUDY.code()).isEqualTo("cloudy");
        assertThat(WeatherCondition.RAINY.code()).isEqualTo("rainy");
        assertThat(WeatherCondition.SNOWY.code()).isEqualTo("snowy");
    }

    @Test
    void 중기_텍스트_매핑_눈은_snowy_우선() {
        assertThat(WeatherCondition.fromMidLandText("흐리고 눈")).isEqualTo(WeatherCondition.SNOWY);
        assertThat(WeatherCondition.fromMidLandText("흐리고 비/눈")).isEqualTo(WeatherCondition.SNOWY);
        assertThat(WeatherCondition.fromMidLandText("흐리고 비")).isEqualTo(WeatherCondition.RAINY);
        assertThat(WeatherCondition.fromMidLandText("구름많음")).isEqualTo(WeatherCondition.CLOUDY);
        assertThat(WeatherCondition.fromMidLandText("맑음")).isEqualTo(WeatherCondition.SUNNY);
    }

    @Test
    void condKo는_강수형태_하늘상태를_한글로() {
        assertThat(WeatherCondition.describeKo(1, 0)).isEqualTo("맑음");
        assertThat(WeatherCondition.describeKo(3, 0)).isEqualTo("구름많음");
        assertThat(WeatherCondition.describeKo(4, 0)).isEqualTo("흐림");
        assertThat(WeatherCondition.describeKo(1, 1)).isEqualTo("비");
        assertThat(WeatherCondition.describeKo(1, 3)).isEqualTo("눈");
        assertThat(WeatherCondition.describeKo(1, 4)).isEqualTo("소나기");
        assertThat(WeatherCondition.describeKo(1, 7)).isEqualTo("눈날림");
    }
}
