package com.haeyaji.be.meeting.dto;

import com.haeyaji.be.meeting.domain.SlotAvailability;

import java.time.LocalDateTime;
import java.util.UUID;

/** 히트맵 셀: 칸 하나의 가능 인원 수. */
public record HeatmapCellResponse(
        UUID slotId,
        LocalDateTime slotStartAt,
        int freeCount
) {

    public static HeatmapCellResponse from(SlotAvailability availability) {
        return new HeatmapCellResponse(availability.slotId(), availability.slotStartAt(), availability.freeCount());
    }
}
