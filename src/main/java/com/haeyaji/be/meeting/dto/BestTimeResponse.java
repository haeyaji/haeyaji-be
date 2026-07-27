package com.haeyaji.be.meeting.dto;

import com.haeyaji.be.meeting.domain.BestTimeResult;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 최적 시간 응답 (MEET-7).
 * <p>{@code revealed=false}면 아직 공개 전이라 {@code windows}가 비어 있다 — 프론트는 결과 대신
 * "{@code respondedCount}/{@code participantCount} 응답, {@code deadline}에 공개" 안내를 띄우면 된다.
 * 공개는 <b>전원 응답</b> 또는 <b>마감 경과</b> 중 먼저 오는 시점이며, 그때까지 응답한 사람만으로 집계한다.
 */
public record BestTimeResponse(
        boolean revealed,
        int maxFreeCount,
        List<TimeWindowResponse> windows,
        int respondedCount,
        int participantCount,
        LocalDateTime deadline
) {

    public static BestTimeResponse from(BestTimeResult result) {
        int maxFreeCount = result.windows().isEmpty() ? 0 : result.windows().getFirst().freeCount();
        return new BestTimeResponse(
                result.revealed(),
                maxFreeCount,
                result.windows().stream().map(TimeWindowResponse::from).toList(),
                result.respondedCount(),
                result.participantCount(),
                result.deadline()
        );
    }
}
