package com.haeyaji.be.meeting.domain;

import java.time.LocalDateTime;

/** 최적 시간 구간 (endAt 배타). */
public record TimeWindow(
        LocalDateTime startAt,
        LocalDateTime endAt,
        int freeCount
) {
}
