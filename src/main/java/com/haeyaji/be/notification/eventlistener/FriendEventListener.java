package com.haeyaji.be.notification.eventlistener;

import com.haeyaji.be.friend.domain.FriendRequestedEvent;
import com.haeyaji.be.friend.domain.FriendRespondedEvent;
import com.haeyaji.be.member.domain.Member;
import com.haeyaji.be.member.repository.MemberRepository;
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
public class FriendEventListener {

    private final NotificationService notificationService;
    private final MemberRepository memberRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRequested(FriendRequestedEvent event) {
        try {
            String requesterNickname = resolveNickname(event.requesterId());
            notificationService.send(
                    event.requesterId(), event.receiverId(),
                    NotificationCategory.FRIEND, NotificationType.FRIEND_REQUEST,
                    requesterNickname, "친구 요청을 보냈습니다.", event.friendId()
            );
        } catch (Exception e) {
            log.error("FRIEND_REQUEST 알림 발송 실패: friendId={}, receiverId={}", event.friendId(), event.receiverId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onResponded(FriendRespondedEvent event) {
        try {
            String responderNickname = resolveNickname(event.responderId());
            String body = event.accepted() ? "친구 요청을 수락했습니다." : "친구 요청을 거절했습니다.";
            notificationService.send(
                    event.responderId(), event.requesterId(),
                    NotificationCategory.FRIEND, NotificationType.FRIEND_RESPONSE,
                    responderNickname, body, event.friendId()
            );
        } catch (Exception e) {
            log.error("FRIEND_RESPONSE 알림 발송 실패: friendId={}, requesterId={}", event.friendId(), event.requesterId(), e);
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
