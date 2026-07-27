package com.haeyaji.be.notification.eventlistener;

import com.haeyaji.be.meeting.domain.MeetingConfirmedEvent;
import com.haeyaji.be.meeting.domain.MeetingInvitedEvent;
import com.haeyaji.be.member.domain.Member;
import com.haeyaji.be.member.repository.MemberRepository;
import com.haeyaji.be.notification.domain.NotificationCategory;
import com.haeyaji.be.notification.domain.NotificationType;
import com.haeyaji.be.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingEventListener {

    private final NotificationService notificationService;
    private final MemberRepository memberRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInvited(MeetingInvitedEvent event) {
        String inviterNickname = resolveNickname(event.inviterMemberId());
        for (UUID inviteeId : event.inviteeMemberIds()) {
            try {
                notificationService.send(
                        event.inviterMemberId(), inviteeId,
                        NotificationCategory.INVITE, NotificationType.MEETING_INVITE,
                        event.meetingTitle(), inviterNickname + "님이 약속에 초대했습니다.", event.meetingId(),
                        event.shareToken()
                );
            } catch (Exception e) { // 500에러 터질 시 알림 발송 전체적으로 이루어지지 않을 수 있음
                log.error("MEETING_INVITE 알림 발송 실패: meetingId={}, inviteeId={}", event.meetingId(), inviteeId, e);
            }

        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConfirmed(MeetingConfirmedEvent event) {
        for (UUID participantId : event.participantMemberIds()) {
            try {
                // MeetingInvitedEvent와 다르게 MeetingConfirmedEvent에는 actor(inviter)가 없음
                // send() 대신 sendSystem으로 알림 발송
                String body = "약속이 확정되었습니다: " + event.confirmedStartAt() + "~" + event.confirmedEndAt();
                notificationService.sendSystem(
                        participantId,
                        NotificationCategory.INVITE, NotificationType.MEETING_CONFIRMED,
                        event.meetingTitle(), body, event.meetingId(),
                        event.shareToken()
                );
            } catch (Exception e) { // 500에러 터질 시 알림 발송 전체적으로 이루어지지 않을 수 있음
                log.error("MEETING_CONFIRMED 알림 발송 실패: meetingId={}, participantId={}", event.meetingId(), participantId, e);
            }

        }
    }

    // 닉네임 온보딩 전(null) 대비 fallback 포함
    private String resolveNickname(UUID memberId) {
        return memberRepository.findById(memberId)
                .map(Member::getNickname)
                .filter(nickname -> nickname != null && !nickname.isBlank())
                .orElse("친구");
    }
}
