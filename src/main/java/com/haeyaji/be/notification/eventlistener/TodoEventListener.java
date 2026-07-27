package com.haeyaji.be.notification.eventlistener;

import com.haeyaji.be.member.domain.Member;
import com.haeyaji.be.member.repository.MemberRepository;
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
    private final MemberRepository memberRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onShared(TodoSharedEvent event) {
        String ownerNickname = resolveNickname(event.ownerId());
        for (UUID inviteeId : event.inviteeMemberIds()) {
            try {
                notificationService.send(
                        event.ownerId(), inviteeId,
                        NotificationCategory.TODO, NotificationType.SHARE_INVITE,
                        event.todoTitle(), ownerNickname + "님이 할 일을 공유했습니다.", event.todoId()
                );
            } catch (Exception e) { // 한 명 발송 실패가 나머지 발송을 막지 않도록 개별 처리
                log.error("SHARE_INVITE 알림 발송 실패: todoId={}, inviteeId={}", event.todoId(), inviteeId, e);
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onResponded(TodoRespondedEvent event) {
        try {
            String responderNickname = resolveNickname(event.responderId());
            String body = responderNickname + "님이 공유 초대를 " + (event.accepted() ? "수락했습니다." : "거절했습니다.");
            notificationService.send(
                    event.responderId(), event.ownerId(),
                    NotificationCategory.TODO, NotificationType.SHARE_INVITE_RESPONSE,
                    event.todoTitle(), body, event.todoId()
            );
        } catch (Exception e) {
            log.error("SHARE_INVITE_RESPONSE 알림 발송 실패: todoId={}, ownerId={}", event.todoId(), event.ownerId(), e);
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
