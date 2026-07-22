package com.haeyaji.be.routine.domain;

import lombok.Builder;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

/**
 * 루틴 도메인 모델 (FR-5). DB 매핑({@code repository.RoutineEntity})과 분리된 순수 객체.
 */
@Builder
public record Routine(
        UUID id,
        String title,
        LocalTime startTime,
        boolean active,
        Set<DayOfWeek> days,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
