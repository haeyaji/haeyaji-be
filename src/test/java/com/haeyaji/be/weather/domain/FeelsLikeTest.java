package com.haeyaji.be.weather.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeelsLikeTest {

    @Test
    void 평온한_날씨는_기온과_동일() {
        assertThat(FeelsLike.calculate(20, 50, 2.0)).isEqualTo(20);
    }

    @Test
    void 무더위는_체감온도가_기온보다_높다() {
        int feels = FeelsLike.calculate(33, 70, 1.0);
        assertThat(feels).isGreaterThanOrEqualTo(33);
    }

    @Test
    void 강풍_추위는_체감온도가_기온보다_낮다() {
        int feels = FeelsLike.calculate(0, 40, 8.0);
        assertThat(feels).isLessThan(0);
    }

    @Test
    void 추워도_바람이_약하면_기온_그대로() {
        assertThat(FeelsLike.calculate(5, 40, 1.0)).isEqualTo(5);
    }
}
