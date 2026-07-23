package com.haeyaji.be.meeting.domain;

import com.haeyaji.be.common.exception.BusinessException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 투표 그리드의 세로축 (MEET-13·14). timeEnd가 timeStart보다 이르면 자정 넘김(익일)으로 해석한다.
 * 경계가 슬롯 단위와 맞지 않으면 보정 없이 거부한다. timeStart == timeEnd(24시간 그리드)는 지원하지 않는다.
 */
public record TimeGrid(
        LocalTime timeStart,
        LocalTime timeEnd,
        int slotUnitMinutes
) {

    public static TimeGrid of(LocalTime timeStart, LocalTime timeEnd, int slotUnitMinutes) {
        if (slotUnitMinutes != 30 && slotUnitMinutes != 60) {
            throw new BusinessException(MeetingErrorCode.INVALID_SLOT_UNIT);
        }
        if (timeStart.equals(timeEnd)) {
            throw new BusinessException(MeetingErrorCode.INVALID_TIME_RANGE);
        }
        if (misaligned(timeStart, slotUnitMinutes) || misaligned(timeEnd, slotUnitMinutes)) {
            throw new BusinessException(MeetingErrorCode.TIME_NOT_ALIGNED);
        }
        return new TimeGrid(timeStart, timeEnd, slotUnitMinutes);
    }

    private static boolean misaligned(LocalTime time, int slotUnitMinutes) {
        return time.getMinute() % slotUnitMinutes != 0 || time.getSecond() != 0 || time.getNano() != 0;
    }

    public boolean crossesMidnight() {
        return timeEnd.isBefore(timeStart) || timeEnd.equals(LocalTime.MIDNIGHT);
    }

    /** 후보 날짜별 [timeStart, timeEnd) 반개구간을 슬롯 단위로 쪼갠 시작 시각 목록. 자정 넘김 슬롯은 date+1로 정규화. */
    public List<LocalDateTime> expandSlotStarts(List<LocalDate> dates) {
        List<LocalDateTime> slotStarts = new ArrayList<>();
        for (LocalDate date : dates) {
            LocalDateTime end = crossesMidnight()
                    ? date.plusDays(1).atTime(timeEnd)
                    : date.atTime(timeEnd);
            for (LocalDateTime cursor = date.atTime(timeStart);
                    cursor.isBefore(end);
                    cursor = cursor.plusMinutes(slotUnitMinutes)) {
                slotStarts.add(cursor);
            }
        }
        return slotStarts.stream().distinct().sorted().toList();
    }

    public boolean isAligned(LocalDateTime at) {
        return !misaligned(at.toLocalTime(), slotUnitMinutes);
    }

    /** [startInclusive, endExclusive) 구간을 슬롯 단위로 쪼갠 시작 시각 목록 (확정 범위 검증용). */
    public List<LocalDateTime> slotStartsBetween(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        List<LocalDateTime> slotStarts = new ArrayList<>();
        for (LocalDateTime cursor = startInclusive;
                cursor.isBefore(endExclusive);
                cursor = cursor.plusMinutes(slotUnitMinutes)) {
            slotStarts.add(cursor);
        }
        return slotStarts;
    }
}
