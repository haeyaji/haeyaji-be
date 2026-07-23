package com.haeyaji.be.meeting.domain;

import com.haeyaji.be.common.exception.ResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 약속(meeting) 도메인 전용 에러 코드.
 */
@Getter
@RequiredArgsConstructor
public enum MeetingErrorCode implements ResponseCode {

    MEETING_NOT_FOUND(HttpStatus.NOT_FOUND, "약속을 찾을 수 없습니다."),
    INVALID_SLOT_UNIT(HttpStatus.BAD_REQUEST, "슬롯 단위는 30분 또는 60분만 허용됩니다."),
    TIME_NOT_ALIGNED(HttpStatus.BAD_REQUEST, "시작·종료 시각이 슬롯 단위 경계와 맞지 않습니다."),
    INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "시작 시각과 종료 시각이 같을 수 없습니다."),
    DUPLICATE_CANDIDATE_DATE(HttpStatus.BAD_REQUEST, "후보 날짜가 중복되었습니다."),
    PAST_CANDIDATE_DATE(HttpStatus.BAD_REQUEST, "과거 날짜는 후보로 지정할 수 없습니다."),
    CANDIDATE_DATE_TOO_FAR(HttpStatus.BAD_REQUEST, "후보 날짜는 오늘부터 2개월 이내여야 합니다."),
    MEETING_EXPIRED(HttpStatus.GONE, "만료된 약속입니다."),
    MEETING_ALREADY_CONFIRMED(HttpStatus.CONFLICT, "이미 확정된 약속입니다."),
    NOT_MEETING_CREATOR(HttpStatus.FORBIDDEN, "약속 생성자만 확정할 수 있습니다."),
    NOT_MEETING_PARTICIPANT(HttpStatus.FORBIDDEN, "약속에 먼저 참여해야 응답할 수 있습니다."),
    INVALID_MEETING_SLOT(HttpStatus.BAD_REQUEST, "약속에 존재하지 않는 시간 칸입니다."),
    INVALID_CONFIRM_RANGE(HttpStatus.BAD_REQUEST, "확정 시간 범위가 올바르지 않습니다."),
    ;

    private final HttpStatus status;
    private final String message;
}
