package com.haeyaji.be.meeting.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/** 약속 참여자. {@code inviteStatus}가 ACCEPTED인 사람만 집계·응답 대상이다. */
public record MeetingParticipant(
        UUID id,
        UUID meetingId,
        UUID memberId,
        LocalDateTime joinedAt,
        InviteStatus inviteStatus
) {
}
