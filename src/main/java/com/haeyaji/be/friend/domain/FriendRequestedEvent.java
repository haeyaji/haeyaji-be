package com.haeyaji.be.friend.domain;

import java.util.UUID;

/**
 * 친구 요청 이벤트 — 알림(noti) 연계 지점. 알림 모듈이 {@code @TransactionalEventListener}로 구독해
 * notification(type=FRIEND_REQUEST, ref_id=friendId)을 만든다.
 *
 * <p>거절했던 요청을 다시 보낸 경우에도 같은 이벤트가 나간다 — 받는 쪽 입장에선 새 요청과 다르지 않다.
 */
public record FriendRequestedEvent(
        UUID friendId,
        UUID requesterId,
        UUID receiverId
) {
}
