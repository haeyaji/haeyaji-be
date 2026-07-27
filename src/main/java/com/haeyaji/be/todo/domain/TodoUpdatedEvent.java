package com.haeyaji.be.todo.domain;

import java.util.List;
import java.util.UUID;

/**
 * 공유 할 일 수정 이벤트 — 알림(noti) 연계 지점. 알림 모듈이 {@code @TransactionalEventListener}로
 * 구독해 수정한 사람을 제외한 나머지 참여자(+owner)에게 notification(type=TODO_SHARED_UPDATED,
 * ref_id=todoId)을 생성한다.
 */
public record TodoUpdatedEvent(
        UUID todoId,
        String todoTitle,
        UUID actorId,
        List<UUID> recipientMemberIds
) {
}
