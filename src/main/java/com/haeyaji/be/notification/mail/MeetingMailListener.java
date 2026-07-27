package com.haeyaji.be.notification.mail;

import com.haeyaji.be.meeting.domain.MeetingConfirmedEvent;
import com.haeyaji.be.meeting.domain.MeetingInvitedEvent;
import com.haeyaji.be.member.domain.Member;
import com.haeyaji.be.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    private final MemberRepository memberRepository;
    private final NotificationMailSender mailSender;
    private final MailTemplates mailTemplates;

    @Value("${app.frontend.callback-url}")
    private String frontendCallbackUrl;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInvited(MeetingInvitedEvent event) {
        String html = mailTemplates.render("meeting-invite", Map.of(
                "meetingTitle", event.meetingTitle(),
                "inviterLine", inviterLine(event.inviterMemberId()),
                "link", meetingLink(event.shareToken())));
        String subject = "[해야지] '%s' 약속에 초대됐어요".formatted(event.meetingTitle());
        emailsOf(event.inviteeMemberIds()).forEach(email -> mailSender.send(email, subject, html));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConfirmed(MeetingConfirmedEvent event) {
        String html = mailTemplates.render("meeting-confirmed", Map.of(
                "meetingTitle", event.meetingTitle(),
                "when", formatWhen(event.confirmedStartAt(), event.confirmedEndAt()),
                "link", meetingLink(event.shareToken())));
        String subject = "[해야지] '%s' 약속 시간이 확정됐어요".formatted(event.meetingTitle());
        // 확정한 방장 본인에게는 보내지 않는다(알림의 NOTI-16과 같은 원칙).
        List<UUID> targets = event.participantMemberIds().stream()
                .filter(id -> !id.equals(event.creatorId()))
                .toList();
        emailsOf(targets).forEach(email -> mailSender.send(email, subject, html));
    }

    /** 초대자 닉네임을 붙인 안내 문구. 닉네임 미설정(온보딩 전)이면 주어 없이 표현한다. */
    private String inviterLine(UUID inviterId) {
        String nickname = inviterId == null ? null : memberRepository.findById(inviterId)
                .map(Member::getNickname)
                .filter(name -> name != null && !name.isBlank())
                .orElse(null);
        return nickname == null ? "약속에 초대됐어요." : "%s님이 약속에 초대했어요.".formatted(nickname);
    }

    /** 이메일을 제공하지 않은 회원(소셜 동의 거부·미입력)은 목록에서 빠진다. */
    private List<String> emailsOf(List<UUID> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return List.of();
        }
        return memberRepository.findAllById(memberIds).stream()
                .map(Member::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .toList();
    }

    private String meetingLink(String shareToken) {
        // 콜백 URL과 같은 오리진의 약속 화면으로 보낸다(fe 라우팅: /meetup/{shareToken}).
        int pathStart = frontendCallbackUrl.indexOf('/', frontendCallbackUrl.indexOf("//") + 2);
        String origin = pathStart > 0 ? frontendCallbackUrl.substring(0, pathStart) : frontendCallbackUrl;
        return origin + "/meetup/" + shareToken;
    }

    private static String formatWhen(LocalDateTime start, LocalDateTime end) {
        if (start == null) {
            return "미정";
        }
        return end == null ? start.format(WHEN) : "%s ~ %s".formatted(start.format(WHEN), end.format(END_TIME));
    }
}
