package com.haeyaji.be.meeting.domain;

import java.time.LocalDate;
import java.util.List;

/** 약속 상세 조립체: 본체 + 후보 날짜 + 투표 칸 + 참여자. */
public record MeetingDetail(
        Meeting meeting,
        List<LocalDate> dates,
        List<MeetingSlot> slots,
        List<MeetingParticipant> participants
) {
}
