package com.haeyaji.be.todo.dto;

import com.haeyaji.be.todo.domain.TodoParticipant;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 공유 참여자 응답 (camelCase).
 */
public record TodoParticipantResponse(
        UUID memberId,
        String role,
        String inviteStatus,
        LocalDateTime createdAt
) {

    public static TodoParticipantResponse from(TodoParticipant participant) {
        return new TodoParticipantResponse(
                participant.memberId(),
                participant.role().name(),
                participant.inviteStatus().name(),
                participant.createdAt()
        );
    }
}
