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

    /**
     * 저장된 요일 조합이 어떤 프리셋(매일·평일·주말)에 해당하는지 판정. 어디에도 안 맞으면 CUSTOM.
     */
    public DayPreset preset() {
        return DayPreset.from(days);
    }
}
