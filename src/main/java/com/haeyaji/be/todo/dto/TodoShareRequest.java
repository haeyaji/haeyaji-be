package com.haeyaji.be.todo.dto;

import com.haeyaji.be.todo.domain.ParticipantRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * 할 일 공유 요청 (SHARE-1). owner만 호출 가능, 여러 명을 한번에 초대한다.
 * role에 OWNER는 지정할 수 없다 — 소유권은 todo.member_id로만 판단하고 여기선 EDITOR/VIEWER만 허용(서비스에서 검증).
 */
public record TodoShareRequest(
        @NotEmpty List<@Valid ShareMember> members
) {
    public record ShareMember(
            @NotNull UUID memberId,
            @NotNull ParticipantRole role
    ) {
    }
}
