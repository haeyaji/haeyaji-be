package com.haeyaji.be.meeting.domain;

/**
 * 약속 참여 상태. 합류 경로가 둘이라 상태로 구분한다.
 * <ul>
 *   <li>{@link #ACCEPTED} — share_token 링크로 직접 합류했거나, 받은 초대를 수락한 상태. <b>집계·응답 권한은 이 상태만</b>.</li>
 *   <li>{@link #PENDING} — 초대만 받고 아직 수락하지 않은 상태. 참여 인원·히트맵 분모에 넣지 않는다.</li>
 *   <li>{@link #REJECTED} — 초대를 거절. 재초대 시 다시 PENDING이 된다.</li>
 * </ul>
 * 할 일 공유({@code todo_participant.invite_status})와 같은 패턴 — 초대를 행으로 남겨야
 * 알림이 유실돼도 "받은 초대"를 다시 찾을 수 있다.
 */
public enum InviteStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}
