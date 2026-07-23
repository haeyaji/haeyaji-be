package com.haeyaji.be.meeting.repository;

import java.util.UUID;

/** 약속별 참여 인원 수 (목록 조회용 프로젝션). */
public record MeetingParticipantCount(
        UUID meetingId,
        long count
) {
}
