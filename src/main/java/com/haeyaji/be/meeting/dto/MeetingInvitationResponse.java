package com.haeyaji.be.meeting.dto;

import com.haeyaji.be.meeting.domain.Meeting;
import com.haeyaji.be.meeting.service.MeetingInvitation;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 받은 약속 초대 1건 (MEET-4). {@code shareToken}으로 참여(수락)하거나 거절할 수 있다.
 */
public record MeetingInvitationResponse(
        UUID meetingId,
        String title,
        String type,
        String shareToken,
        LocalDateTime deadline,
        LocalDateTime createdAt
) {

    public static MeetingInvitationResponse from(MeetingInvitation invitation) {
        Meeting meeting = invitation.meeting();
        return new MeetingInvitationResponse(
                meeting.id(),
                meeting.title(),
                meeting.type().name(),
                invitation.shareToken(),
                meeting.deadline(),
                meeting.createdAt()
        );
    }
}
