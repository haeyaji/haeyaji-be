package com.haeyaji.be.notification.mail;

import com.haeyaji.be.meeting.domain.MeetingConfirmedEvent;
import com.haeyaji.be.meeting.domain.MeetingInvitedEvent;
import com.haeyaji.be.member.domain.Member;
import com.haeyaji.be.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 약속 이벤트를 메일로 알린다 (초대·확정).
 *
 * <p><b>커밋 이후</b>에만 보낸다({@link TransactionPhase#AFTER_COMMIT}) — 약속 저장이 롤백됐는데
 * "초대됐다"는 메일이 나가는 일을 막기 위함. 발송 자체는 {@link NotificationMailSender}가 비동기·실패흡수로 처리한다.
 *
 * <p>알림(notification) 도메인이 붙으면 같은 이벤트에 DB 저장 리스너가 추가로 달릴 수 있다.
 * 리스너끼리는 독립이라 함께 동작한다(이쪽은 메일, 그쪽은 알림함).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingMailListener {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("M월 d일 HH:mm");

    private final MemberRepository memberRepository;
    private final NotificationMailSender mailSender;

    @Value("${app.frontend.callback-url}")
    private String frontendCallbackUrl;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInvited(MeetingInvitedEvent event) {
        String link = meetingLink(event.shareToken());
        for (String email : emailsOf(event.inviteeMemberIds())) {
            mailSender.send(email,
                    "[해야지] '%s' 약속에 초대됐어요".formatted(event.meetingTitle()),
                    """
                    <p><b>%s</b> 약속에 초대됐어요.</p>
                    <p>가능한 시간을 골라주면 모두에게 맞는 시간을 찾아드릴게요.</p>
                    <p><a href="%s">약속 보러 가기</a></p>
                    """.formatted(escape(event.meetingTitle()), link));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConfirmed(MeetingConfirmedEvent event) {
        String link = meetingLink(event.shareToken());
        String when = formatWhen(event.confirmedStartAt(), event.confirmedEndAt());
        for (String email : emailsOf(event.participantMemberIds())) {
            mailSender.send(email,
                    "[해야지] '%s' 약속 시간이 확정됐어요".formatted(event.meetingTitle()),
                    """
                    <p><b>%s</b> 약속 시간이 확정됐어요.</p>
                    <p>일시: <b>%s</b></p>
                    <p><a href="%s">약속 보러 가기</a></p>
                    """.formatted(escape(event.meetingTitle()), when, link));
        }
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
        return end == null ? start.format(WHEN) : "%s ~ %s".formatted(start.format(WHEN), end.toLocalTime());
    }

    /** 제목은 사용자 입력이라 메일 HTML에 그대로 넣지 않는다. */
    private static String escape(String raw) {
        return raw == null ? "" : raw.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
