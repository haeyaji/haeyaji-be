package com.haeyaji.be.todo.domain;

/**
 * 공유 참여자 권한. OWNER > EDITOR > VIEWER 순으로 강함.
 * 소유자는 todo.member_id로만 판단하고 todo_participant엔 OWNER 행을 두지 않으므로,
 * 실제로 참여자 초대·역할변경에 쓰이는 값은 EDITOR/VIEWER뿐이다 (OWNER 지정 시 서비스에서 거부).
 */
public enum ParticipantRole {
    OWNER,
    EDITOR,
    VIEWER;

    /** 이 role이 required 이상의 권한을 갖는지 (숫자가 작을수록 강함). */
    public boolean isAtLeast(ParticipantRole required) {
        return this.ordinal() <= required.ordinal();
    }
}
