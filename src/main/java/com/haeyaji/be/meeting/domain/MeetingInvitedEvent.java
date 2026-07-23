package com.haeyaji.be.meeting.domain;

import java.util.List;
import java.util.UUID;

/**
 * 약속 초대 이벤트 — 알림(noti) 연계 지점. 알림 모듈이 {@code @TransactionalEventListener}로
 * 구독해 notification(type=MEETING_INVITE, ref_id=meetingId, link_token=shareToken)을 생성한다.
 * inviteeMemberIds는 회원 실존 검증 없이 전달되며, 수신자 검증은 구독 측 책임이다.
 */
public record MeetingInvitedEvent(
        UUID meetingId,
        String shareToken,
        String meetingTitle,
        UUID inviterMemberId,
        List<UUID> inviteeMemberIds
) {
}
