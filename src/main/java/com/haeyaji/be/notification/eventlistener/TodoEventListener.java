package com.haeyaji.be.notification.eventlistener;

import com.haeyaji.be.notification.domain.NotificationCategory;
import com.haeyaji.be.notification.domain.NotificationType;
import com.haeyaji.be.notification.service.NotificationService;
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
                        event.todoTitle(), "eventbody", event.todoId(),
                        null // linkToken — todo는 공개 공유링크 없음
                );
            } catch (Exception e) { // 한 명 발송 실패가 나머지 발송을 막지 않도록 개별 처리
                log.error("SHARE_INVITE 알림 발송 실패: todoId={}, inviteeId={}", event.todoId(), inviteeId, e);
            }
        }
    }
}
