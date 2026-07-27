package com.haeyaji.be.meeting.domain;

import java.util.UUID;

/**
 * 약속 삭제 이벤트 — 약속에서 파생된 것들을 지우라는 신호.
 *
 * <p>확정된 약속은 참여자 캘린더에 공유 할 일로 퍼져 있고 알림함에도 링크가 남아 있다.
 * 약속 행만 지우면 그것들이 <b>돌아갈 곳 없는 유령</b>으로 남으므로, todo·알림 모듈이 이 이벤트를
 * 구독해 각자 정리한다 — meeting 모듈이 남의 테이블을 직접 건드리지 않게 하는 것도 같은 이유다.
 *
 * <p>확정과 마찬가지로 같은 트랜잭션에서 처리한다(‘약속은 사라졌는데 할 일은 남은’ 중간 상태 방지).
 */
public record MeetingDeletedEvent(
        UUID meetingId,
        String meetingTitle
) {
}
