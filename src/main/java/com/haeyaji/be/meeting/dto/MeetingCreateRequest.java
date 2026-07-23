package com.haeyaji.be.meeting.dto;

import com.haeyaji.be.meeting.domain.MeetingType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 약속 생성 요청 (MEET-1). deadline은 선택(null=무제한). 생성자는 인증 principal에서 얻는다.
 * 슬롯 단위·시각 정렬 검증은 {@code domain.TimeGrid}에서 수행한다 (MEET-13·14).
 * 후보 날짜 검증(중복·과거·2개월 한도)은 {@code domain.CandidateDates}에서 수행한다.
 */
public record MeetingCreateRequest(
        @NotBlank @Size(max = 100) String title,
        @NotNull MeetingType type,
        @NotEmpty List<@NotNull LocalDate> dates,
        @NotNull LocalTime timeStart,
        @NotNull LocalTime timeEnd,
        @NotNull Integer slotUnitMinutes,
        @Future LocalDateTime deadline
) {
}
