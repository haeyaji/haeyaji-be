package com.haeyaji.be.todo.domain;

import java.util.List;
import java.util.UUID;

/**
 * 할 일 공유 이벤트 — 알림(noti) 연계 지점. 알림 모듈이 {@code @TransactionalEventListener}로
 * 구독해 notification(type=SHARE_INVITE, ref_id=todoId)을 생성한다.
 */
public record TodoSharedEvent(
        UUID todoId,
        String todoTitle,
        UUID ownerId,
        List<UUID> inviteeMemberIds
) {
}
