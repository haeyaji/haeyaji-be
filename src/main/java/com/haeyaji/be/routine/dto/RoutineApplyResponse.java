package com.haeyaji.be.routine.dto;

/**
 * 루틴 일괄 등록 결과 (ROUT-6). 중복 스킵 후 실제로 생성된 todo 개수.
 */
public record RoutineApplyResponse(int created) {
}
