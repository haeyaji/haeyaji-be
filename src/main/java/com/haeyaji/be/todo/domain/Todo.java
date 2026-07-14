package com.haeyaji.be.todo.domain;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 할 일 도메인 모델 (FR-1). DB 매핑({@code repository.TodoEntity})과 분리된 순수 객체.
 */
@Builder
public record Todo(
        Long id,
        String title,
        LocalDate todoDate,
        LocalTime startTime,
        String location,
        String category,
        TodoSource source,
        Long sourceRefId,
        TodoStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
