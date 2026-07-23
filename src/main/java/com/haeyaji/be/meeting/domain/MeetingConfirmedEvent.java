package com.haeyaji.be.meeting.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 약속 확정 이벤트 — 알림(noti) 연계 지점. 알림 모듈이 {@code @TransactionalEventListener}로
 * 구독해 참여자 전원에게 notification(type=MEETING_CONFIRMED, ref_id=meetingId,
 * link_token=shareToken)을 생성한다. confirmedEndAt은 배타 경계.
 */
public record MeetingConfirmedEvent(
        UUID meetingId,
        String shareToken,
        String meetingTitle,
        LocalDateTime confirmedStartAt,
        LocalDateTime confirmedEndAt,
        List<UUID> participantMemberIds
) {
}
