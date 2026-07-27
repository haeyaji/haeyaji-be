package com.haeyaji.be.notification.eventlistener;

import com.haeyaji.be.notification.domain.NotificationCategory;
import com.haeyaji.be.notification.domain.NotificationType;
import com.haeyaji.be.notification.service.NotificationService;
import com.haeyaji.be.todo.domain.TodoRespondedEvent;
import com.haeyaji.be.todo.domain.TodoSharedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TodoEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onShared(TodoSharedEvent event) {
        for (UUID inviteeId : event.inviteeMemberIds()) {
            try {
                notificationService.send(
                        event.ownerId(), inviteeId,
                        NotificationCategory.TODO, NotificationType.SHARE_INVITE,
                        event.todoTitle(), "할 일을 공유받았습니다.", event.todoId()
                );
            } catch (Exception e) { // 한 명 발송 실패가 나머지 발송을 막지 않도록 개별 처리
                log.error("SHARE_INVITE 알림 발송 실패: todoId={}, inviteeId={}", event.todoId(), inviteeId, e);
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onResponded(TodoRespondedEvent event) {
        try {
            String body = event.accepted() ? "공유 초대를 수락했습니다." : "공유 초대를 거절했습니다.";
            notificationService.send(
                    event.responderId(), event.ownerId(),
                    NotificationCategory.TODO, NotificationType.SHARE_INVITE_RESPONSE,
                    event.todoTitle(), body, event.todoId()
            );
        } catch (Exception e) {
            log.error("SHARE_INVITE_RESPONSE 알림 발송 실패: todoId={}, ownerId={}", event.todoId(), event.ownerId(), e);
        }
    }
}
