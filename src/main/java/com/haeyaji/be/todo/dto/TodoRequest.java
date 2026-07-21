package com.haeyaji.be.todo.dto;

import com.haeyaji.be.todo.domain.TodoSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 할 일 추가 요청. title/todoDate는 필수, 나머지는 선택.
 * source 미지정 시 서비스에서 {@link TodoSource#MANUAL}로 채운다.
 * 길이 제한은 {@code TodoEntity} 컬럼 길이(title 100/placeName 100/placeUrl 300/category 30)와 맞춘다.
 */
public record TodoRequest(
        @NotBlank @Size(max = 100) String title,
        @NotNull LocalDate todoDate,
        LocalTime startTime,
        @Size(max = 100) String placeName,
        @Size(max = 300) String placeUrl,
        Double lat,
        Double lng,
        @Size(max = 30) String category,
        TodoSource source
) {
}
