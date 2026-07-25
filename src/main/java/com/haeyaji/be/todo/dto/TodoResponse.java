package com.haeyaji.be.todo.dto;

import com.haeyaji.be.todo.domain.ParticipantRole;
import com.haeyaji.be.todo.domain.Todo;
import com.haeyaji.be.todo.domain.TodoStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * 할 일 응답 (camelCase).
 * <p>{@code shared}가 true면 공유받은 할 일(내 소유 아님), {@code myRole}은 그때의 내 권한(EDITOR/VIEWER).
 * VIEWER면 프론트가 편집을 잠근다(완료 토글·수정은 EDITOR 이상만).
 */
public record TodoResponse(
        UUID id,
        String title,
        LocalDate date,
        LocalTime time,
        LocalDateTime endedAt,
        String placeName,
        String placeUrl,
        Double lat,
        Double lng,
        UUID labelId,
        String source,
        UUID sourceRefId,
        boolean completed,
        boolean pinned,
        int sortOrder,
        boolean shared,
        String myRole
) {

    public static TodoResponse from(Todo todo) {
        return from(todo, null);
    }

    /** @param sharedRole null이면 내 소유(shared=false), 아니면 공유받은 것과 내 권한. */
    public static TodoResponse from(Todo todo, ParticipantRole sharedRole) {
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
                todo.labelId(),
                todo.source().name(),
                todo.sourceRefId(),
                todo.status() == TodoStatus.DONE,
                todo.pinned(),
                todo.sortOrder(),
                sharedRole != null,
                sharedRole != null ? sharedRole.name() : null
        );
    }
}
