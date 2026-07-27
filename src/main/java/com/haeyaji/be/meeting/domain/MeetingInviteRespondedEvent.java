package com.haeyaji.be.meeting.domain;

import java.util.UUID;

/**
 * 약속 초대 응답 이벤트 — 알림(noti) 연계 지점. 알림 모듈이 구독해
 * notification(type=MEETING_INVITE_RESPONSE, ref_id=meetingId)을 <b>방장</b>에게 만든다.
 *
 * <p>방장은 "누가 들어왔는지"를 알아야 마감을 기다릴지 먼저 확정할지 판단할 수 있다.
 * 초대 없이 공유 URL로 그냥 합류한 경우엔 발행하지 않는다 — 초대에 대한 응답이 아니기 때문.
 */
public record MeetingInviteRespondedEvent(
        UUID meetingId,
        String shareToken,
        String meetingTitle,
        UUID creatorId,
        UUID responderId,
        boolean accepted
) {
}
