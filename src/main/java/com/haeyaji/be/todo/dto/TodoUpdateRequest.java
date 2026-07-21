package com.haeyaji.be.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

/**
 * 할 일 수정 요청. 제목/시간/장소/분류만 수정 가능 (날짜·출처·상태는 이 API로 안 바뀜).
 * 길이 제한은 {@code TodoEntity} 컬럼 길이(title 100/location 200/category 30)와 맞춘다.
 */
public record TodoUpdateRequest(
        @NotBlank @Size(max = 100) String title,
        LocalTime startTime,
        @Size(max = 200) String location,
        @Size(max = 30) String category
) {
}
