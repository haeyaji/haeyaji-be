package com.haeyaji.be.meeting.domain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 최적 시간 집계 결과 (MEET-7).
 * <p><b>공개 조건</b>: 수락한 참여자가 <b>전원 응답</b>했거나, <b>응답 마감(deadline)이 지났을 때</b> 둘 중 먼저 오는 시점.
 * 그전까지는 {@code revealed=false}이고 {@code windows}는 비어 있다 — 일부만 응답한 중간 결과를
 * 최종 추천처럼 보여주면 오해를 부르기 때문. 히트맵은 그와 무관하게 실시간으로 조회된다.
 * <p>공개 시점의 집계 대상은 "그때까지 응답한 사람"이다. 미응답자를 기다리지 않는다.
 *
 * @param revealed         공개 여부
 * @param windows          가능 인원이 최대인 연속 구간(미공개면 빈 목록)
 * @param respondedCount   응답을 제출한 사람 수
 * @param participantCount 수락한 참여자 수(응답 대상 모수)
 * @param deadline         응답 마감 — 이 시각이 지나면 전원 응답이 아니어도 공개된다
 */
public record BestTimeResult(
        boolean revealed,
        List<TimeWindow> windows,
        int respondedCount,
        int participantCount,
        LocalDateTime deadline
) {

    public static BestTimeResult hidden(int respondedCount, int participantCount, LocalDateTime deadline) {
        return new BestTimeResult(false, List.of(), respondedCount, participantCount, deadline);
    }

    public static BestTimeResult revealed(List<TimeWindow> windows, int respondedCount,
                                          int participantCount, LocalDateTime deadline) {
        return new BestTimeResult(true, windows, respondedCount, participantCount, deadline);
    }
}
