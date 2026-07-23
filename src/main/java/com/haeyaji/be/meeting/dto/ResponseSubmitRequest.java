package com.haeyaji.be.meeting.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 가능 시간 제출 요청 (MEET-5). full-replace — 빈 목록이면 해당 회원의 응답 전체 삭제.
 * 요청 회원은 인증 principal에서 얻는다.
 */
public record ResponseSubmitRequest(
        @NotNull @Valid List<SlotResponseItem> responses
) {
}
