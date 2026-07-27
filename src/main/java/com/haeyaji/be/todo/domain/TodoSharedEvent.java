package com.haeyaji.be.todo.domain;

import java.util.List;
import java.util.UUID;

/**
 * 할 일 공유 초대 이벤트 — 알림(noti)·메일 연계 지점. 알림 모듈이 구독해
 * notification(type=SHARE_INVITE, ref_id=todoId)을 초대받은 사람들에게 만든다.
 *
 * <p>초대는 {@code todo_participant}에 PENDING 행으로도 남으므로, 알림이 유실돼도
 * 초대함(GET /todos/invitations)에서 다시 찾을 수 있다 — 알림은 발견을 빠르게 할 뿐이다.
 *
 * <p>권한을 사람별로 싣는 이유: 한 번의 공유에서 사람마다 권한이 다를 수 있고,
 * 메일 본문이 "받은 권한"을 안내하므로 받는 사람마다 맞는 값이어야 한다.
 */
public record TodoSharedEvent(
        UUID todoId,
        String todoTitle,
        UUID ownerId,
        List<Invitee> invitees
) {

    public record Invitee(UUID memberId, ParticipantRole role) {
    }

    public List<UUID> inviteeMemberIds() {
        return invitees.stream().map(Invitee::memberId).toList();
    }
}
