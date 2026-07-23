package com.haeyaji.be.routine.domain;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DayPresetTest {

    @Test
    void 일곱개_다_있으면_매일() {
        assertThat(DayPreset.from(Set.of(DayOfWeek.values()))).isEqualTo(DayPreset.DAILY);
    }

    @Test
    void 월화수목금이면_평일() {
        Set<DayOfWeek> weekdays = Set.of(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

        assertThat(DayPreset.from(weekdays)).isEqualTo(DayPreset.WEEKDAY);
    }

    @Test
    void 토일이면_주말() {
        assertThat(DayPreset.from(Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))).isEqualTo(DayPreset.WEEKEND);
    }

    @Test
    void 그외_조합이면_커스텀() {
        assertThat(DayPreset.from(Set.of(DayOfWeek.MONDAY))).isEqualTo(DayPreset.CUSTOM);
        assertThat(DayPreset.from(Set.of(DayOfWeek.MONDAY, DayOfWeek.SATURDAY))).isEqualTo(DayPreset.CUSTOM);
    }
}
