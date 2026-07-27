package com.haeyaji.be.notification.eventlistener;

import com.haeyaji.be.friend.domain.FriendRequestedEvent;
import com.haeyaji.be.friend.domain.FriendRespondedEvent;
import com.haeyaji.be.notification.domain.NotificationCategory;
import com.haeyaji.be.notification.domain.NotificationType;
import com.haeyaji.be.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 친구 요청·응답을 알림함에 남긴다. 친구 도메인엔 알림 UI로 가는 다른 경로가 없어
 * (요청 목록을 직접 열어보지 않으면 모른다) 알림이 유일한 발견 수단이다.
 *
 * <p>커밋 이후에만 발송한다 — 요청 저장이 롤백됐는데 알림만 남는 일을 막기 위함.
 * 알림 하나가 실패해도 나머지 흐름을 막지 않도록 예외는 여기서 삼킨다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FriendEventListener {

    private final NotificationService notificationService;
    private final ActorNameResolver actorNames;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRequested(FriendRequestedEvent event) {
        try {
            notificationService.send(
                    event.requesterId(), event.receiverId(),
                    NotificationCategory.FRIEND, NotificationType.FRIEND_REQUEST,
                    "새 친구 요청",
                    "%s님이 친구 요청을 보냈어요.".formatted(actorNames.nicknameOf(event.requesterId())),
                    event.friendId(), null
            );
        } catch (Exception e) {
            log.error("FRIEND_REQUEST 알림 발송 실패: friendId={}, receiverId={}",
                    event.friendId(), event.receiverId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onResponded(FriendRespondedEvent event) {
        String responder = actorNames.nicknameOf(event.responderId());
        String title = event.accepted() ? "친구가 됐어요" : "친구 요청이 거절됐어요";
        String body = event.accepted()
                ? "%s님과 친구가 됐어요. 이제 일정을 함께 나눌 수 있어요.".formatted(responder)
                : "%s님이 친구 요청을 거절했어요.".formatted(responder);
        try {
            notificationService.send(
                    event.responderId(), event.requesterId(),
                    NotificationCategory.FRIEND, NotificationType.FRIEND_RESPONSE,
                    title, body, event.friendId(), null
            );
        } catch (Exception e) {
            log.error("FRIEND_RESPONSE 알림 발송 실패: friendId={}, requesterId={}",
                    event.friendId(), event.requesterId(), e);
        }
    }
}
