package com.haeyaji.be.friend.domain;

import java.util.UUID;

/**
 * 친구 관계 + <b>상대방</b>의 id·닉네임. 목록을 그리려면 이름이 필요한데, 관계 행에는 id만 있다.
 *
 * <p>"상대방"은 보는 사람 기준이다 — 내가 보낸 요청이면 수신자가, 받은 요청이면 요청자가 상대다.
 * 이 판정과 이름 결합을 <b>DB 조회 한 번</b>에서 끝낸다(조회 따로·이름 따로 받아 앱에서 합치면
 * 왕복이 두 번이 되고, 맞추는 일을 DB 대신 앱이 한다).
 *
 * <p>닉네임은 온보딩 전 회원이면 {@code null}이다.
 */
public record FriendWithCounterpart(
        Friend friend,
        UUID counterpartId,
        String counterpartNickname
) {
}
