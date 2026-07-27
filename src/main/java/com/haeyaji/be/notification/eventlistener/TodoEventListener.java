package com.haeyaji.be.notification.eventlistener;

import com.haeyaji.be.notification.domain.NotificationCategory;
import com.haeyaji.be.notification.domain.NotificationType;
import com.haeyaji.be.notification.service.NotificationService;
import com.haeyaji.be.todo.domain.SharedTodoUpdatedEvent;
import com.haeyaji.be.todo.domain.TodoShareRespondedEvent;
import com.haeyaji.be.todo.domain.TodoSharedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * 할 일 공유 관련 알림 — 초대·응답·수정.
 *
 * <p>커밋 이후에만 발송한다 — 공유 저장이 롤백됐는데 "초대됐다"는 알림만 남는 일을 막기 위함.
 * 수신자 한 명의 실패가 나머지를 막지 않도록 예외는 건별로 삼킨다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TodoEventListener {

    private final NotificationService notificationService;
    private final ActorNameResolver actorNames;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onShared(TodoSharedEvent event) {
        String body = "%s님이 '%s' 일정에 초대했어요.".formatted(
                actorNames.nicknameOf(event.ownerId()), event.todoTitle());
        for (UUID inviteeId : event.inviteeMemberIds()) {
            send(NotificationType.SHARE_INVITE, event.ownerId(), inviteeId,
                    "새 일정 초대", body, event.todoId());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onShareResponded(TodoShareRespondedEvent event) {
        String responder = actorNames.nicknameOf(event.responderId());
        String title = event.accepted() ? "초대를 수락했어요" : "초대를 거절했어요";
        String body = "%s님이 '%s' 일정 초대를 %s.".formatted(
                responder, event.todoTitle(), event.accepted() ? "수락했어요" : "거절했어요");
        send(NotificationType.SHARE_INVITE_RESPONSE, event.responderId(), event.ownerId(),
                title, body, event.todoId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSharedTodoUpdated(SharedTodoUpdatedEvent event) {
        String body = "%s님이 '%s' 일정을 수정했어요.".formatted(
                actorNames.nicknameOf(event.actorId()), event.todoTitle());
        for (UUID memberId : event.audienceMemberIds()) {
            // 수정한 본인은 send()가 걸러준다(NOTI-16).
            send(NotificationType.TODO_SHARED_UPDATED, event.actorId(), memberId,
                    "공유 일정이 바뀌었어요", body, event.todoId());
        }
    }

    private void send(NotificationType type, UUID actorId, UUID memberId,
                      String title, String body, UUID refId) {
        try {
            notificationService.send(actorId, memberId,
                    categoryOf(type), type, title, body, refId, null);
        } catch (Exception e) {
            log.error("{} 알림 발송 실패: refId={}, memberId={}", type, refId, memberId, e);
        }
    }

    private static NotificationCategory categoryOf(NotificationType type) {
        // 초대·응답은 '초대함' 묶음, 내용 변경은 '할 일' 묶음으로 본다(fe 탭 분류와 일치).
        return type == NotificationType.TODO_SHARED_UPDATED
                ? NotificationCategory.TODO
                : NotificationCategory.INVITE;
    }
}
