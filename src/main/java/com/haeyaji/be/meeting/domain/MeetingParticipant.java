package com.haeyaji.be.meeting.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/** 약속 참여자. */
public record MeetingParticipant(
        UUID id,
        UUID meetingId,
        UUID memberId,
        LocalDateTime joinedAt
) {
}
