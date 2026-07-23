package com.haeyaji.be.meeting.dto;

import com.haeyaji.be.meeting.domain.Meeting;
import com.haeyaji.be.meeting.domain.MeetingSummary;

import java.time.LocalDateTime;
import java.util.UUID;

/** 약속 목록 한 줄 응답 (MEET-11). */
public record MeetingSummaryResponse(
        UUID id,
        String title,
        String type,
        String status,
        String shareToken,
        long participantCount,
        LocalDateTime deadline,
        LocalDateTime confirmedStartAt,
        LocalDateTime confirmedEndAt,
        LocalDateTime createdAt
) {

    public static MeetingSummaryResponse from(MeetingSummary summary) {
        Meeting meeting = summary.meeting();
        return new MeetingSummaryResponse(
                meeting.id(),
                meeting.title(),
                meeting.type().name(),
                meeting.status().name(),
                meeting.shareToken(),
                summary.participantCount(),
                meeting.deadline(),
                meeting.confirmedStartAt(),
                meeting.confirmedEndAt(),
                meeting.createdAt()
        );
    }
}
