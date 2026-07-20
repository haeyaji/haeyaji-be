package com.haeyaji.be.todo.dto;

import com.haeyaji.be.todo.domain.TodoSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 할 일 추가 요청. title/todoDate는 필수, 나머지는 선택.
 * source 미지정 시 서비스에서 {@link TodoSource#MANUAL}로 채운다.
 */
public record TodoRequest(
        @NotBlank String title,
        @NotNull LocalDate todoDate,
        LocalTime startTime,
        String location,
        String category,
        TodoSource source
) {
}
