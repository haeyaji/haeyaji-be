package com.haeyaji.be.friend.domain;

import java.util.UUID;

/**
 * 친구 요청 이벤트 — 알림(noti) 연계 지점. 알림 모듈이 {@code @TransactionalEventListener}로
 * 구독해 notification(type=FRIEND_REQUEST, ref_id=friendId)을 생성한다.
 */
public record FriendRequestedEvent(
        UUID friendId,
        UUID requesterId,
        UUID receiverId
) {
}
