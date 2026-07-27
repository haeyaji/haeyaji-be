package com.haeyaji.be.meeting.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 약속 수정 요청 (MEET-15). 부분 수정 — 안 보낸 필드(null)는 그대로 둔다.
 *
 * <p>유형({@code type})은 못 바꾼다 — 바꿔야 할 만큼 성격이 다르면 새로 만드는 편이 낫고,
 * 참여자가 들어온 뒤 성격이 바뀌면 왜 초대됐는지가 흐려진다.
 *
 * <p>시간 관련 필드({@code dates}·{@code timeStart}·{@code timeEnd}·{@code slotUnitMinutes})는
 * 하나만 바꿔도 시간 칸 전체를 다시 깔아야 하므로, 서비스에서 <b>모두 함께</b> 받도록 요구한다.
 * 검증 규칙(정렬·중복·과거·2개월 한도)은 생성과 같은 {@code TimeGrid}·{@code CandidateDates}를 탄다.
 */
public record MeetingUpdateRequest(
        @Size(max = 100) String title,
        // 안 보내면(null) 그대로 두되, 보냈다면 빈 배열은 후보 날짜가 사라진다는 뜻이라 막는다.
        @Size(min = 1) List<@NotNull LocalDate> dates,
        LocalTime timeStart,
        LocalTime timeEnd,
        Integer slotUnitMinutes,
        @Future LocalDateTime deadline
) {

    /** 시간 칸을 다시 깔아야 하는 수정인지. */
    public boolean touchesTimeGrid() {
        return dates != null || timeStart != null || timeEnd != null || slotUnitMinutes != null;
    }

    /** 시간 칸을 다시 깔려면 네 값이 모두 있어야 한다 — 일부만 받으면 남은 값과 어긋난 격자가 나온다. */
    public boolean hasCompleteTimeGrid() {
        return dates != null && timeStart != null && timeEnd != null && slotUnitMinutes != null;
    }
}
