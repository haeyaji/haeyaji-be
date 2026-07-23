package com.haeyaji.be.meeting.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/** 투표 칸(그리드 셀) 하나. */
public record MeetingSlot(
        UUID id,
        LocalDateTime slotStartAt
) {
}
