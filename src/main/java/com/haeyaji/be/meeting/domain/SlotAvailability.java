package com.haeyaji.be.meeting.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/** 칸 하나의 가능 인원 수 (히트맵 셀). */
public record SlotAvailability(
        UUID slotId,
        LocalDateTime slotStartAt,
        int freeCount
) {
}
