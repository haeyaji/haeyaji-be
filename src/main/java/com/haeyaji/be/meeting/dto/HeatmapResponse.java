package com.haeyaji.be.meeting.dto;

import com.haeyaji.be.meeting.domain.MeetingAvailability;

import java.util.List;

/** 시간대별 가능 인원 히트맵 응답 (MEET-6). */
public record HeatmapResponse(
        int participantCount,
        List<HeatmapCellResponse> cells
) {

    public static HeatmapResponse from(MeetingAvailability availability) {
        return new HeatmapResponse(
                availability.participantCount(),
                availability.slots().stream().map(HeatmapCellResponse::from).toList()
        );
    }
}
