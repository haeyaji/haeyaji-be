package com.haeyaji.be.meeting.dto;

import com.haeyaji.be.meeting.domain.TimeWindow;

import java.time.LocalDateTime;

/** 최적 시간 구간 응답 (endAt 배타). */
public record TimeWindowResponse(
        LocalDateTime startAt,
        LocalDateTime endAt,
        int freeCount
) {

    public static TimeWindowResponse from(TimeWindow window) {
        return new TimeWindowResponse(window.startAt(), window.endAt(), window.freeCount());
    }
}
