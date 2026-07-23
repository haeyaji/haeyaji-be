package com.haeyaji.be.todo.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 할 일 공유 참여자 도메인 모델. DB 매핑({@code repository.TodoParticipantEntity})과 분리된 순수 객체.
 */
public record TodoParticipant(
        UUID id,
        UUID todoId,
        UUID memberId,
        ParticipantRole role,
        InviteStatus inviteStatus,
        LocalDateTime createdAt
) {
}
