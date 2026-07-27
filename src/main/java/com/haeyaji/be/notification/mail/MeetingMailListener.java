package com.haeyaji.be.notification.mail;

import com.haeyaji.be.meeting.domain.MeetingConfirmedEvent;
import com.haeyaji.be.meeting.domain.MeetingInvitedEvent;
import com.haeyaji.be.notification.eventlistener.ActorNameResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 약속 이벤트를 메일로 알린다 (초대·확정). 본문은 {@link MailTemplates}의 브랜드 템플릿을 쓴다
 * (원본·디자인 가이드: {@code docs/email-templates.html}).
 *
 * <p><b>커밋 이후</b>에만 보낸다({@link TransactionPhase#AFTER_COMMIT}) — 약속 저장이 롤백됐는데
 * "초대됐다"는 메일이 나가는 일을 막기 위함. 발송 자체는 {@link NotificationMailSender}가 비동기·실패흡수로 처리한다.
 *
 * <p>알림 도메인의 {@code MeetingEventListener}가 같은 이벤트로 알림함을 채운다.
 * 리스너끼리는 독립이라 함께 동작한다(이쪽은 메일, 그쪽은 알림함·SSE).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingMailListener {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("M월 d일 HH:mm");
    private static final DateTimeFormatter END_TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final MemberMails memberMails;
    private final ActorNameResolver actorNames;
    private final NotificationMailSender mailSender;
    private final MailTemplates mailTemplates;
    private final MailLinks mailLinks;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInvited(MeetingInvitedEvent event) {
        String html = mailTemplates.render("haeyaji-meeting-invite", Map.of(
                "meetingTitle", event.meetingTitle(),
                "inviterName", actorNames.nicknameOf(event.inviterMemberId()),
                "link", mailLinks.meeting(event.shareToken())));
        String subject = "[해야지] '%s' 약속에 초대됐어요".formatted(event.meetingTitle());
        memberMails.of(event.inviteeMemberIds()).forEach(email -> mailSender.send(email, subject, html));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConfirmed(MeetingConfirmedEvent event) {
        String html = mailTemplates.render("haeyaji-meeting-confirmed", Map.of(
                "meetingTitle", event.meetingTitle(),
                "when", formatWhen(event.confirmedStartAt(), event.confirmedEndAt()),
                "link", mailLinks.meeting(event.shareToken())));
        String subject = "[해야지] '%s' 약속 시간이 확정됐어요".formatted(event.meetingTitle());
        // 확정한 방장 본인에게는 보내지 않는다(알림의 NOTI-16과 같은 원칙).
        List<UUID> targets = event.participantMemberIds().stream()
                .filter(id -> !id.equals(event.creatorId()))
                .toList();
        memberMails.of(targets).forEach(email -> mailSender.send(email, subject, html));
    }

    private static String formatWhen(LocalDateTime start, LocalDateTime end) {
        if (start == null) {
            return "미정";
        }
        return end == null ? start.format(WHEN) : "%s ~ %s".formatted(start.format(WHEN), end.format(END_TIME));
    }
}
