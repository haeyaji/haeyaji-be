package com.haeyaji.be.meeting.domain;

import java.util.List;
import java.util.UUID;

/**
 * 초대 처리 결과. invited는 초대 이벤트가 발행된 회원, skipped는 이미 참여 중이라 제외된 회원.
 */
public record MeetingInviteResult(
        List<UUID> invitedMemberIds,
        List<UUID> skippedMemberIds
) {
}
