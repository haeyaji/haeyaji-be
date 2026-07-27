package com.haeyaji.be.todo.domain;

import java.util.UUID;

/**
 * 할 일 공유 초대 응답(수락/거절) 이벤트 — 알림(noti) 연계 지점. 알림 모듈이
 * {@code @TransactionalEventListener}로 구독해 owner에게 notification(type=SHARE_INVITE_RESPONSE,
 * ref_id=todoId)을 생성한다.
 */
public record TodoRespondedEvent(
        UUID todoId,
        String todoTitle,
        UUID responderId,
        UUID ownerId,
        boolean accepted
) {
}
