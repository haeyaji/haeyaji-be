package com.haeyaji.be.meeting.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 약속 확정 요청 (MEET-9). confirmedEndAt은 배타 경계. 요청 회원은 인증 principal에서 얻는다.
 */
public record MeetingConfirmRequest(
        @NotNull LocalDateTime confirmedStartAt,
        @NotNull LocalDateTime confirmedEndAt
) {
}
