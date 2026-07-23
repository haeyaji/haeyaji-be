package com.haeyaji.be.todo.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.UUID;

/**
 * 할 일 수정 요청. 제목·시간·장소·라벨·완료 여부를 한 번에 처리한다 (날짜·출처는 이 API로 안 바뀜).
 * 부분 수정(partial patch) 방식 — 필드를 안 보내면(null) 기존 값을 그대로 유지한다.
 * 명시적으로 비우고 싶어도 이 API로는 안 되고(값을 지우는 기능은 아직 없음), 값이 있을 때만 갱신된다.
 * 길이 제한은 {@code TodoEntity} 컬럼 길이(title 100/placeName 100/placeUrl 300)와 맞춘다.
 */
public record TodoUpdateRequest(
        @Size(max = 100) String title,
        LocalTime time,
        @Size(max = 100) String placeName,
        @Size(max = 300) String placeUrl,
        @DecimalMin("-90") @DecimalMax("90") Double lat,
        @DecimalMin("-180") @DecimalMax("180") Double lng,
        UUID labelId,
        Boolean pinned,
        Integer sortOrder,
        Boolean completed
) {
}
