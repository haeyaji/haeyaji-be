package com.haeyaji.be.meeting.dto;

import com.haeyaji.be.meeting.domain.TimeWindow;

import java.util.List;

/** 최적 시간 응답 (MEET-7). 응답자가 없으면 maxFreeCount=0, windows는 빈 목록. */
public record BestTimeResponse(
        int maxFreeCount,
        List<TimeWindowResponse> windows
) {

    public static BestTimeResponse from(List<TimeWindow> windows) {
        int maxFreeCount = windows.isEmpty() ? 0 : windows.getFirst().freeCount();
        return new BestTimeResponse(
                maxFreeCount,
                windows.stream().map(TimeWindowResponse::from).toList()
        );
    }
}
