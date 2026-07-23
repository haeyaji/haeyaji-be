package com.haeyaji.be.meeting.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MeetingAvailabilityTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 8, 1, 9, 0);
    private static final UUID M1 = UUID.randomUUID();
    private static final UUID M2 = UUID.randomUUID();

    private static MeetingSlot slot(int index, int unitMinutes) {
        return new MeetingSlot(UUID.randomUUID(), BASE.plusMinutes((long) index * unitMinutes));
    }

    private static SlotResponse free(MeetingSlot slot, UUID memberId) {
        return new SlotResponse(UUID.randomUUID(), slot.id(), memberId, ResponseStatus.FREE);
    }

    private static SlotResponse busy(MeetingSlot slot, UUID memberId) {
        return new SlotResponse(UUID.randomUUID(), slot.id(), memberId, ResponseStatus.BUSY);
    }

    @Test
    void FREE만_집계하고_BUSY와_미응답은_0으로_본다() {
        MeetingSlot s0 = slot(0, 30);
        MeetingSlot s1 = slot(1, 30);
        MeetingSlot s2 = slot(2, 30);

        MeetingAvailability availability = MeetingAvailability.of(
                List.of(s0, s1, s2),
                List.of(free(s0, M1), busy(s1, M1), free(s0, M2)),
                2);

        assertThat(availability.slots()).extracting(SlotAvailability::freeCount)
                .containsExactly(2, 0, 0);
    }

    @Test
    void 최대_인원의_연속_구간을_반환하고_끝은_배타() {
        MeetingSlot s0 = slot(0, 30);
        MeetingSlot s1 = slot(1, 30);
        MeetingSlot s2 = slot(2, 30);
        MeetingSlot s3 = slot(3, 30);

        MeetingAvailability availability = MeetingAvailability.of(
                List.of(s0, s1, s2, s3),
                List.of(free(s0, M1),
                        free(s1, M1), free(s1, M2),
                        free(s2, M1), free(s2, M2)),
                2);

        List<TimeWindow> windows = availability.bestWindows(30);

        assertThat(windows).containsExactly(
                new TimeWindow(s1.slotStartAt(), s2.slotStartAt().plusMinutes(30), 2));
    }

    @Test
    void 연속이_끊기면_구간이_분리된다() {
        MeetingSlot s0 = slot(0, 30);
        MeetingSlot s1 = slot(1, 30);
        MeetingSlot s2 = slot(2, 30);

        MeetingAvailability availability = MeetingAvailability.of(
                List.of(s0, s1, s2),
                List.of(free(s0, M1), free(s2, M1)),
                1);

        assertThat(availability.bestWindows(30)).containsExactly(
                new TimeWindow(s0.slotStartAt(), s0.slotStartAt().plusMinutes(30), 1),
                new TimeWindow(s2.slotStartAt(), s2.slotStartAt().plusMinutes(30), 1));
    }

    @Test
    void 날짜가_다른_슬롯은_시간이_이어져도_별개_구간() {
        MeetingSlot day1Last = new MeetingSlot(UUID.randomUUID(), LocalDateTime.of(2026, 8, 1, 23, 30));
        MeetingSlot day3First = new MeetingSlot(UUID.randomUUID(), LocalDateTime.of(2026, 8, 3, 0, 0));

        MeetingAvailability availability = MeetingAvailability.of(
                List.of(day1Last, day3First),
                List.of(free(day1Last, M1), free(day3First, M1)),
                1);

        assertThat(availability.bestWindows(30)).hasSize(2);
    }

    @Test
    void 응답이_없으면_최적_구간도_없다() {
        MeetingAvailability availability = MeetingAvailability.of(
                List.of(slot(0, 30), slot(1, 30)), List.of(), 2);

        assertThat(availability.maxFreeCount()).isZero();
        assertThat(availability.bestWindows(30)).isEmpty();
    }
}
