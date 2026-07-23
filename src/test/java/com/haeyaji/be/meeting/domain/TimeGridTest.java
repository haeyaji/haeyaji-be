package com.haeyaji.be.meeting.domain;

import com.haeyaji.be.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeGridTest {

    @Test
    void 슬롯_단위는_30분_또는_60분만_허용() {
        assertThatThrownBy(() -> TimeGrid.of(LocalTime.of(9, 0), LocalTime.of(12, 0), 45))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(MeetingErrorCode.INVALID_SLOT_UNIT);
    }

    @Test
    void 시작과_종료가_같으면_거부() {
        assertThatThrownBy(() -> TimeGrid.of(LocalTime.of(9, 0), LocalTime.of(9, 0), 30))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(MeetingErrorCode.INVALID_TIME_RANGE);
    }

    @Test
    void 단위_경계에_맞지_않는_시각은_보정_없이_거부() {
        assertThatThrownBy(() -> TimeGrid.of(LocalTime.of(9, 10), LocalTime.of(12, 0), 30))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(MeetingErrorCode.TIME_NOT_ALIGNED);
        // 60분 단위에서는 30분 경계도 미정렬
        assertThatThrownBy(() -> TimeGrid.of(LocalTime.of(9, 30), LocalTime.of(12, 0), 60))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(MeetingErrorCode.TIME_NOT_ALIGNED);
    }

    @Test
    void 날짜별로_반개구간을_단위로_쪼갠다() {
        TimeGrid grid = TimeGrid.of(LocalTime.of(9, 0), LocalTime.of(11, 0), 30);
        LocalDate date = LocalDate.of(2026, 8, 1);

        List<LocalDateTime> slots = grid.expandSlotStarts(List.of(date));

        assertThat(slots).containsExactly(
                date.atTime(9, 0), date.atTime(9, 30), date.atTime(10, 0), date.atTime(10, 30));
    }

    @Test
    void 자정을_넘기면_익일로_이어진다() {
        TimeGrid grid = TimeGrid.of(LocalTime.of(23, 0), LocalTime.of(1, 0), 30);
        LocalDate date = LocalDate.of(2026, 8, 1);

        List<LocalDateTime> slots = grid.expandSlotStarts(List.of(date));

        assertThat(grid.crossesMidnight()).isTrue();
        assertThat(slots).containsExactly(
                date.atTime(23, 0), date.atTime(23, 30),
                date.plusDays(1).atTime(0, 0), date.plusDays(1).atTime(0, 30));
    }

    @Test
    void 종료가_자정이면_당일_끝까지_생성() {
        TimeGrid grid = TimeGrid.of(LocalTime.of(23, 0), LocalTime.MIDNIGHT, 30);
        LocalDate date = LocalDate.of(2026, 8, 1);

        assertThat(grid.expandSlotStarts(List.of(date)))
                .containsExactly(date.atTime(23, 0), date.atTime(23, 30));
    }

    @Test
    void 정렬_판정은_분과_초를_모두_본다() {
        TimeGrid grid = TimeGrid.of(LocalTime.of(9, 0), LocalTime.of(12, 0), 30);

        assertThat(grid.isAligned(LocalDateTime.of(2026, 8, 1, 9, 30))).isTrue();
        assertThat(grid.isAligned(LocalDateTime.of(2026, 8, 1, 9, 15))).isFalse();
        assertThat(grid.isAligned(LocalDateTime.of(2026, 8, 1, 9, 30, 1))).isFalse();
    }

    @Test
    void 구간을_슬롯_시작_시각들로_쪼갠다() {
        TimeGrid grid = TimeGrid.of(LocalTime.of(9, 0), LocalTime.of(12, 0), 30);

        List<LocalDateTime> starts = grid.slotStartsBetween(
                LocalDateTime.of(2026, 8, 1, 9, 30), LocalDateTime.of(2026, 8, 1, 10, 30));

        assertThat(starts).containsExactly(
                LocalDateTime.of(2026, 8, 1, 9, 30), LocalDateTime.of(2026, 8, 1, 10, 0));
    }
}
