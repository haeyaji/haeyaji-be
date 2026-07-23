package com.haeyaji.be.meeting.dto;

import com.haeyaji.be.meeting.domain.MeetingParticipant;

import java.time.LocalDateTime;
import java.util.UUID;

/** 참여자 응답. */
public record ParticipantResponse(
        UUID id,
        UUID memberId,
        LocalDateTime joinedAt
) {

    public static ParticipantResponse from(MeetingParticipant participant) {
        return new ParticipantResponse(participant.id(), participant.memberId(), participant.joinedAt());
    }
}
