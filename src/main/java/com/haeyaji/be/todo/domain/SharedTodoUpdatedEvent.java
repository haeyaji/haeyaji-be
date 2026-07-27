package com.haeyaji.be.todo.domain;

import java.util.List;
import java.util.UUID;

/**
 * 공유 중인 할 일이 수정된 이벤트 — 알림(noti) 연계 지점. 알림 모듈이 구독해
 * notification(type=TODO_SHARED_UPDATED, ref_id=todoId)을 <b>수정한 사람을 뺀</b> 관련자에게 만든다.
 *
 * <p>혼자 쓰는 할 일에는 발행하지 않는다 — 수신자가 자기 자신뿐이라 알릴 대상이 없다.
 * 누구를 뺄지는 {@code actorId}로 구독 측이 판단한다.
 */
public record SharedTodoUpdatedEvent(
        UUID todoId,
        String todoTitle,
        UUID actorId,
        List<UUID> audienceMemberIds
) {
}
