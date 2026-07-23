package com.haeyaji.be.todo.dto;

import com.haeyaji.be.todo.domain.ParticipantRole;
import jakarta.validation.constraints.NotNull;

/**
 * 참여자 역할 변경 요청 (SHARE-4). owner만 호출 가능, OWNER로는 변경 불가(서비스에서 검증).
 */
public record TodoParticipantRoleUpdateRequest(
        @NotNull ParticipantRole role
) {
}
