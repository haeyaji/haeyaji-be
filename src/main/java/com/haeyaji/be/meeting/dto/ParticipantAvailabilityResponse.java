package com.haeyaji.be.meeting.dto;

import com.haeyaji.be.meeting.domain.ParticipantResponses;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** 참여자 한 명의 응답 현황 (MEET-8). */
public record ParticipantAvailabilityResponse(
        UUID memberId,
        LocalDateTime joinedAt,
        boolean responded,
        List<SlotResponseItem> responses
) {

    public static ParticipantAvailabilityResponse from(ParticipantResponses participantResponses) {
        return new ParticipantAvailabilityResponse(
                participantResponses.participant().memberId(),
                participantResponses.participant().joinedAt(),
                participantResponses.responded(),
                participantResponses.responses().stream().map(SlotResponseItem::from).toList()
        );
    }
}
