package com.haeyaji.be.meeting.dto;

import com.haeyaji.be.meeting.domain.MeetingSlot;

import java.time.LocalDateTime;
import java.util.UUID;

/** 투표 칸 응답. */
public record TimeSlotResponse(
        UUID id,
        LocalDateTime slotStartAt
) {

    public static TimeSlotResponse from(MeetingSlot slot) {
        return new TimeSlotResponse(slot.id(), slot.slotStartAt());
    }
}
