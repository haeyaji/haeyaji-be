package com.haeyaji.be.meeting.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MeetingTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 1, 12, 0);

    private static Meeting meeting(MeetingStatus status, LocalDateTime deadline) {
        return Meeting.builder()
                .id(UUID.randomUUID())
                .creatorId(UUID.randomUUID())
                .title("테스트")
                .type(MeetingType.CASUAL)
                .timeStart(LocalTime.of(9, 0))
                .timeEnd(LocalTime.of(12, 0))
                .slotUnitMinutes(30)
                .deadline(deadline)
                .status(status)
                .shareToken("token")
                .createdAt(NOW.minusDays(1))
                .build();
    }

    @Test
    void 마감이_지난_수집중_약속은_만료로_판정() {
        Meeting meeting = meeting(MeetingStatus.COLLECTING, NOW.minusMinutes(1));

        assertThat(meeting.statusAt(NOW)).isEqualTo(MeetingStatus.EXPIRED);
        assertThat(meeting.resolveStatus(NOW).status()).isEqualTo(MeetingStatus.EXPIRED);
    }

    @Test
    void 마감_전이거나_마감이_없으면_수집중_유지() {
        assertThat(meeting(MeetingStatus.COLLECTING, NOW.plusMinutes(1)).statusAt(NOW))
                .isEqualTo(MeetingStatus.COLLECTING);
        assertThat(meeting(MeetingStatus.COLLECTING, null).statusAt(NOW))
                .isEqualTo(MeetingStatus.COLLECTING);
    }

    @Test
    void 확정된_약속은_마감이_지나도_만료되지_않는다() {
        Meeting meeting = meeting(MeetingStatus.CONFIRMED, NOW.minusDays(1));

        assertThat(meeting.statusAt(NOW)).isEqualTo(MeetingStatus.CONFIRMED);
    }

    @Test
    void 상태가_그대로면_동일_인스턴스를_반환() {
        Meeting meeting = meeting(MeetingStatus.COLLECTING, null);

        assertThat(meeting.resolveStatus(NOW)).isSameAs(meeting);
    }

    @Test
    void 생성자_판별() {
        Meeting meeting = meeting(MeetingStatus.COLLECTING, null);

        assertThat(meeting.isCreator(meeting.creatorId())).isTrue();
        assertThat(meeting.isCreator(UUID.randomUUID())).isFalse();
    }
}
