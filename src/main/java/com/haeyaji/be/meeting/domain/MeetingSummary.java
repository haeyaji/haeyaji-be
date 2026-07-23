package com.haeyaji.be.meeting.domain;

/** 약속 목록 한 줄: 본체 + 참여 인원 수 (MEET-11). */
public record MeetingSummary(
        Meeting meeting,
        long participantCount
) {
}
