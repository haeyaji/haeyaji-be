package com.haeyaji.be.notification.mail;

import com.haeyaji.be.notification.eventlistener.ActorNameResolver;
import com.haeyaji.be.todo.domain.ParticipantRole;
import com.haeyaji.be.todo.domain.TodoSharedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * 할 일 공유 초대를 메일로 알린다 (템플릿 {@code haeyaji-todo-share}).
 *
 * <p>커밋 이후에만 보낸다 — 공유가 롤백됐는데 "초대됐다"는 메일이 나가는 일을 막기 위함.
 * 알림함을 채우는 {@code TodoEventListener}와는 서로 독립이라 함께 동작한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TodoMailListener {

    private final MemberMails memberMails;
    private final ActorNameResolver actorNames;
    private final NotificationMailSender mailSender;
    private final MailTemplates mailTemplates;
    private final MailLinks mailLinks;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onShared(TodoSharedEvent event) {
        String inviterName = actorNames.nicknameOf(event.ownerId());
        String subject = "[해야지] '%s' 할 일에 초대됐어요".formatted(event.todoTitle());
        // 권한이 사람마다 다를 수 있어 본문을 한 번 만들어 돌려쓰지 않는다.
        for (TodoSharedEvent.Invitee invitee : event.invitees()) {
            String email = memberMails.of(invitee.memberId());
            if (email == null) {
                continue; // 이메일을 안 준 회원 — 알림함으로는 이미 전달된다
            }
            String html = mailTemplates.render("haeyaji-todo-share", Map.of(
                    "todoTitle", event.todoTitle(),
                    "inviterName", inviterName,
                    "roleLabel", label(invitee.role()),
                    "link", mailLinks.app()));
            mailSender.send(email, subject, html);
        }
    }

    private static String label(ParticipantRole role) {
        return role == ParticipantRole.VIEWER ? "보기 전용" : "함께 편집";
    }
}
