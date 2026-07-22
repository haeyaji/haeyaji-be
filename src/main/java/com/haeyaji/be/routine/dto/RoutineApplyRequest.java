package com.haeyaji.be.routine.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 루틴 일괄 등록 요청 (ROUT-4). 지정 기간(from~to, 둘 다 포함) 안의 활성 루틴을 todo로 펼친다.
 */
public record RoutineApplyRequest(
        @NotNull LocalDate from,
        @NotNull LocalDate to
) {
}
