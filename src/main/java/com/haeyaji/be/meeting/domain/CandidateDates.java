package com.haeyaji.be.meeting.domain;

import com.haeyaji.be.common.exception.BusinessException;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 약속 후보 날짜 목록 (MEET-1). 중복 없이, 오늘부터 2개월 뒤까지(양끝 포함)의 날짜만 허용한다.
 * 검증을 통과하면 오름차순으로 정렬해 보관한다.
 */
public record CandidateDates(List<LocalDate> dates) {

    private static final int MAX_MONTHS_AHEAD = 2;

    public static CandidateDates of(List<LocalDate> dates, LocalDate today) {
        if (Set.copyOf(dates).size() != dates.size()) {
            throw new BusinessException(MeetingErrorCode.DUPLICATE_CANDIDATE_DATE);
        }
        if (dates.stream().anyMatch(date -> date.isBefore(today))) {
            throw new BusinessException(MeetingErrorCode.PAST_CANDIDATE_DATE);
        }
        LocalDate limit = today.plusMonths(MAX_MONTHS_AHEAD);
        if (dates.stream().anyMatch(date -> date.isAfter(limit))) {
            throw new BusinessException(MeetingErrorCode.CANDIDATE_DATE_TOO_FAR);
        }
        return new CandidateDates(dates.stream().sorted().toList());
    }
}
