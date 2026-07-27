package com.haeyaji.be.todo.domain;

import java.util.List;
import java.util.UUID;

/**
 * 할 일 공유 초대 이벤트 — 알림(noti) 연계 지점. 알림 모듈이 구독해
 * notification(type=SHARE_INVITE, ref_id=todoId)을 초대받은 사람들에게 만든다.
 *
 * <p>초대는 {@code todo_participant}에 PENDING 행으로도 남으므로, 알림이 유실돼도
 * 초대함(GET /todos/invitations)에서 다시 찾을 수 있다 — 알림은 발견을 빠르게 할 뿐이다.
 */
public record TodoSharedEvent(
        UUID todoId,
        String todoTitle,
        UUID ownerId,
        List<UUID> inviteeMemberIds
) {
}
