package com.haeyaji.be.routine.dto;

import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

/**
 * 루틴 수정 요청 (ROUT-3). 제목·시간·반복요일·활성여부·라벨을 한 번에 처리한다.
 * 부분 수정(partial patch) 방식 — 필드를 안 보내면(null) 기존 값을 그대로 유지한다.
 * days를 보낼 땐 최소 1개 이상이어야 한다(서비스에서 검증) — 반복요일 없는 루틴은 의미가 없음.
 */
public record RoutineUpdateRequest(
        @Size(max = 100) String title,
        LocalTime startTime,
        Set<DayOfWeek> days,
        Boolean active,
        UUID labelId
) {
}
