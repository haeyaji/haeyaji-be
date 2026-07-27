package com.haeyaji.be.friend.domain;

import java.util.UUID;

/**
 * 친구 요청 응답 이벤트 — 알림(noti) 연계 지점. 알림 모듈이 구독해
 * notification(type=FRIEND_RESPONSE, ref_id=friendId)을 <b>요청을 보냈던 쪽</b>에 만든다.
 *
 * <p>거절({@code accepted=false})도 발행한다 — 요청을 보낸 사람은 어느 쪽이든 결과를 알아야
 * 다시 보낼지 판단할 수 있다. 문구를 가르는 건 구독 측 책임이다.
 */
public record FriendRespondedEvent(
        UUID friendId,
        UUID responderId,
        UUID requesterId,
        boolean accepted
) {
}
