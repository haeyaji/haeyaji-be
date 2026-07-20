package com.haeyaji.be.todo.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalTime;

/**
 * 할 일 수정 요청. 제목/시간/장소/분류만 수정 가능 (날짜·출처·상태는 이 API로 안 바뀜).
 */
public record TodoUpdateRequest(
        @NotBlank String title,
        LocalTime startTime,
        String location,
        String category
) {
}
