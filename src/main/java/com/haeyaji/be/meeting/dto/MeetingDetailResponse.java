package com.haeyaji.be.meeting.dto;

import com.haeyaji.be.meeting.domain.Meeting;
import com.haeyaji.be.meeting.domain.MeetingDetail;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * 약속 상세 응답 (camelCase). status는 서비스에서 조회 시점 유효 상태로 치환되어 온다.
 */
public record MeetingDetailResponse(
        UUID id,
        UUID creatorId,
        String title,
        String type,
        String status,
        LocalTime timeStart,
        LocalTime timeEnd,
        int slotUnitMinutes,
        LocalDateTime deadline,
        LocalDateTime confirmedStartAt,
        LocalDateTime confirmedEndAt,
        String shareToken,
        List<LocalDate> dates,
        List<TimeSlotResponse> slots,
        List<ParticipantResponse> participants,
        LocalDateTime createdAt
) {

    public static MeetingDetailResponse from(MeetingDetail detail) {
        Meeting meeting = detail.meeting();
        return new MeetingDetailResponse(
                meeting.id(),
                meeting.creatorId(),
                meeting.title(),
                meeting.type().name(),
                meeting.status().name(),
                meeting.timeStart(),
                meeting.timeEnd(),
                meeting.slotUnitMinutes(),
                meeting.deadline(),
                meeting.confirmedStartAt(),
                meeting.confirmedEndAt(),
                meeting.shareToken(),
                detail.dates(),
                detail.slots().stream().map(TimeSlotResponse::from).toList(),
                detail.participants().stream().map(ParticipantResponse::from).toList(),
                meeting.createdAt()
        );
    }
}
