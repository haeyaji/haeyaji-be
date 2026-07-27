package com.haeyaji.be.meeting.service;

import com.haeyaji.be.meeting.domain.Meeting;

/**
 * 내가 받은 대기(PENDING) 약속 초대 1건. 수락하려면 {@code shareToken}으로 참여(join)하면 된다.
 */
public record MeetingInvitation(Meeting meeting, String shareToken) {
}
