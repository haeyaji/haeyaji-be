package com.haeyaji.be.routine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

/**
 * 루틴 등록 요청 (ROUT-1). title/days는 필수, startTime·labelId는 선택.
 * days는 프리셋(매일/평일/주말) 변환이 끝난 최종 요일 집합을 받는다 — 프리셋 변환은 fe 담당(ROUT-2).
 * 길이 제한은 {@code RoutineEntity} 컬럼 길이(title 100)와 맞춘다.
 * memberId는 인증 미구현으로 아직 요청에 안 받음(추후 로그인 붙을 때 추가).
 */
public record RoutineRequest(
        @NotBlank @Size(max = 100) String title,
        LocalTime startTime,
        @NotEmpty Set<DayOfWeek> days,
        UUID labelId
) {
}
