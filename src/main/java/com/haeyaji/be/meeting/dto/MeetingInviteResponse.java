package com.haeyaji.be.meeting.dto;

import com.haeyaji.be.meeting.domain.MeetingInviteResult;

import java.util.List;
import java.util.UUID;

/** 약속 초대 응답. skipped는 이미 참여 중이라 초대에서 제외된 회원. */
public record MeetingInviteResponse(
        List<UUID> invitedMemberIds,
        List<UUID> skippedMemberIds
) {

    public static MeetingInviteResponse from(MeetingInviteResult result) {
        return new MeetingInviteResponse(result.invitedMemberIds(), result.skippedMemberIds());
    }
}
