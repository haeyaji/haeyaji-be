package com.haeyaji.be.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

/**
 * 할 일 수정 요청. 제목·시간·장소·분류·완료 여부를 한 번에 처리한다 (날짜·출처는 이 API로 안 바뀜).
 * {@code completed}가 안 오면(null) 완료 상태는 그대로 유지.
 * 길이 제한은 {@code TodoEntity} 컬럼 길이(title 100/placeName 100/placeUrl 300/category 30)와 맞춘다.
 */
public record TodoUpdateRequest(
        @NotBlank @Size(max = 100) String title,
        LocalTime time,
        @Size(max = 100) String placeName,
        @Size(max = 300) String placeUrl,
        Double lat,
        Double lng,
        @Size(max = 30) String category,
        Boolean pinned,
        Integer sortOrder,
        Boolean completed
) {
}
