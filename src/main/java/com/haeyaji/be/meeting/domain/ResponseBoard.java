package com.haeyaji.be.meeting.domain;

import java.util.List;

/** 응답 현황판: 참여자별 응답 목록 (MEET-8). */
public record ResponseBoard(
        int participantCount,
        List<ParticipantResponses> participants
) {

    public int respondedCount() {
        return (int) participants.stream().filter(ParticipantResponses::responded).count();
    }
}
