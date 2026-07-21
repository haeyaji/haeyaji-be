package com.haeyaji.be.todo.domain;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * 할 일 도메인 모델 (FR-1). DB 매핑({@code repository.TodoEntity})과 분리된 순수 객체.
 */
@Builder
public record Todo(
        UUID id,
        String title,
        LocalDate todoDate,
        LocalTime startTime,
        LocalTime endTime,
        String placeName,
        String placeUrl,
        Double lat,
        Double lng,
        String category,
        TodoSource source,
        UUID sourceRefId,
        TodoStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
