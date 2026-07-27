package com.haeyaji.be.meeting.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 약속 확정 이벤트. 두 곳에서 구독한다.
 * <ul>
 *   <li>할 일 전환(MEET-10): todo 모듈이 확정 약속을 공유 할 일 1건으로 만든다(생성자=owner, 참여자=editor).</li>
 *   <li>알림(noti) 연계: 알림 모듈이 참여자 전원에게 notification(type=MEETING_CONFIRMED,
 *       ref_id=meetingId, link_token=shareToken)을 생성한다.</li>
 * </ul>
 * {@code confirmedEndAt}은 배타 경계. {@code participantMemberIds}에는 생성자도 포함된다.
 */
public record MeetingConfirmedEvent(
        UUID meetingId,
        String shareToken,
        String meetingTitle,
        UUID creatorId,
        LocalDateTime confirmedStartAt,
        LocalDateTime confirmedEndAt,
        List<UUID> participantMemberIds
) {
}
