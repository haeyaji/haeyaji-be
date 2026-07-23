package com.haeyaji.be.todo.dto;

import com.haeyaji.be.todo.domain.TodoSource;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * 할 일 추가 요청. title/date는 필수, 나머지는 선택.
 * source 미지정 시 서비스에서 {@link TodoSource#MANUAL}로 채운다.
 * 길이 제한은 {@code TodoEntity} 컬럼 길이(title 100/placeName 100/placeUrl 300)와 맞춘다.
 * memberId는 인증 미구현으로 아직 요청에 안 받음(추후 로그인 붙을 때 추가).
 */
public record TodoRequest(
        @NotBlank @Size(max = 100) String title,
        @NotNull LocalDate date,
        LocalTime time,
        @Size(max = 100) String placeName,
        @Size(max = 300) String placeUrl,
        @DecimalMin("-90") @DecimalMax("90") Double lat,
        @DecimalMin("-180") @DecimalMax("180") Double lng,
        UUID labelId,
        TodoSource source,
        Boolean pinned,
        Integer sortOrder
) {
}
