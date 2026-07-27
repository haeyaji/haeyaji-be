package com.haeyaji.be.notification.eventlistener;

import com.haeyaji.be.meeting.domain.MeetingConfirmedEvent;
import com.haeyaji.be.meeting.domain.MeetingInvitedEvent;
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
    // Todo: EventBody 어떻게 채울 것인지?
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInvited(MeetingInvitedEvent event) {
        for (UUID inviteeId : event.inviteeMemberIds()) {
            try {
                notificationService.send(
                        event.inviterMemberId(), inviteeId,
                        NotificationCategory.INVITE, NotificationType.MEETING_INVITE,
                        event.meetingTitle(), "가능한 시간을 선택해 주세요.", event.meetingId(),
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
                notificationService.send(
                        event.creatorId(), // 확정한 사람(방장) — 본인에게는 발송되지 않는다(NOTI-16)
                        participantId,
                        NotificationCategory.INVITE, NotificationType.MEETING_CONFIRMED,
                        event.meetingTitle(), "약속 시간이 확정됐어요.", event.meetingId(),
                        event.shareToken()
                );
            } catch (Exception e) { // 500에러 터질 시 알림 발송 전체적으로 이루어지지 않을 수 있음
                log.error("MEETING_CONFIRMED 알림 발송 실패: meetingId={}, participantId={}", event.meetingId(), participantId, e);
            }

        }
    }
}
