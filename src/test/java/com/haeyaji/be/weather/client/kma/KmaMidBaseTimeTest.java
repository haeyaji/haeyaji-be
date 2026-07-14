package com.haeyaji.be.weather.client.kma;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class KmaMidBaseTimeTest {

    @Test
    void 오전7시_이후면_오늘_06시_발표를_쓴다() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 1, 8, 0);
        assertThat(KmaMidBaseTime.baseDate(now)).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(KmaMidBaseTime.resolveTmFc(now)).isEqualTo("202607010600");
    }

    @Test
    void 오전7시_이전이면_전날_06시_발표를_쓴다() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 1, 5, 0);
        assertThat(KmaMidBaseTime.baseDate(now)).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(KmaMidBaseTime.resolveTmFc(now)).isEqualTo("202606300600");
    }
}
