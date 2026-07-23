package com.haeyaji.be.meeting.domain;

import java.util.List;

/** 참여자 한 명의 응답 묶음 (MEET-8). */
public record ParticipantResponses(
        MeetingParticipant participant,
        List<SlotResponse> responses
) {

    public boolean responded() {
        return !responses.isEmpty();
    }
}
