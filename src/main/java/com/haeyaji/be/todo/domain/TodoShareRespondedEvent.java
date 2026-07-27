package com.haeyaji.be.todo.domain;

import java.util.UUID;

/**
 * 할 일 공유 초대에 대한 응답 이벤트 — 알림(noti) 연계 지점. 알림 모듈이 구독해
 * notification(type=SHARE_INVITE_RESPONSE, ref_id=todoId)을 <b>초대한 주인</b>에게 만든다.
 *
 * <p>거절({@code accepted=false})도 발행한다 — 주인은 상대가 안 들어온 이유를 알아야
 * 다시 초대할지 판단할 수 있다.
 */
public record TodoShareRespondedEvent(
        UUID todoId,
        String todoTitle,
        UUID ownerId,
        UUID responderId,
        boolean accepted
) {
}
