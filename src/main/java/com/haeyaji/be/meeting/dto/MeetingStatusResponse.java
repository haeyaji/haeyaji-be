package com.haeyaji.be.meeting.dto;

import com.haeyaji.be.meeting.domain.ResponseBoard;

import java.util.List;

/** 응답 현황 응답 (MEET-8). */
public record MeetingStatusResponse(
        int participantCount,
        int respondedCount,
        List<ParticipantAvailabilityResponse> participants
) {

    public static MeetingStatusResponse from(ResponseBoard board) {
        return new MeetingStatusResponse(
                board.participantCount(),
                board.respondedCount(),
                board.participants().stream().map(ParticipantAvailabilityResponse::from).toList()
        );
    }
}
