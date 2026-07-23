package com.haeyaji.be.routine.repository;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DayOfWeekConverterTest {

    private final DayOfWeekConverter converter = new DayOfWeekConverter();

    @Test
    void 요일을_DB_3글자_약어로_변환한다() {
        assertThat(converter.convertToDatabaseColumn(DayOfWeek.MONDAY)).isEqualTo("MON");
        assertThat(converter.convertToDatabaseColumn(DayOfWeek.TUESDAY)).isEqualTo("TUE");
        assertThat(converter.convertToDatabaseColumn(DayOfWeek.WEDNESDAY)).isEqualTo("WED");
        assertThat(converter.convertToDatabaseColumn(DayOfWeek.THURSDAY)).isEqualTo("THU");
        assertThat(converter.convertToDatabaseColumn(DayOfWeek.FRIDAY)).isEqualTo("FRI");
        assertThat(converter.convertToDatabaseColumn(DayOfWeek.SATURDAY)).isEqualTo("SAT");
        assertThat(converter.convertToDatabaseColumn(DayOfWeek.SUNDAY)).isEqualTo("SUN");
    }

    @Test
    void DB_3글자_약어를_요일로_변환한다() {
        assertThat(converter.convertToEntityAttribute("MON")).isEqualTo(DayOfWeek.MONDAY);
        assertThat(converter.convertToEntityAttribute("SUN")).isEqualTo(DayOfWeek.SUNDAY);
    }

    @Test
    void null은_그대로_null로_왕복한다() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void 알수없는_약어는_예외() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("XXX"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
