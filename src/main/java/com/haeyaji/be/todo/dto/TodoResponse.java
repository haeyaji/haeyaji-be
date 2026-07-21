package com.haeyaji.be.todo.dto;

import com.haeyaji.be.todo.domain.Todo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * 할 일 응답 (camelCase).
 */
public record TodoResponse(
        UUID id,
        String title,
        LocalDate todoDate,
        LocalTime startTime,
        LocalDateTime endedAt,
        String placeName,
        String placeUrl,
        Double lat,
        Double lng,
        String category,
        String source,
        UUID sourceRefId,
        String status
) {

    public static TodoResponse from(Todo todo) {
        return new TodoResponse(
                todo.id(),
                todo.title(),
                todo.todoDate(),
                todo.startTime(),
                todo.endedAt(),
                todo.placeName(),
                todo.placeUrl(),
                todo.lat(),
                todo.lng(),
                todo.category(),
                todo.source().name(),
                todo.sourceRefId(),
                todo.status().name()
        );
    }
}
