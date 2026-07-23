package com.haeyaji.be.meeting.service;

import com.haeyaji.be.meeting.domain.MeetingParticipant;
import com.haeyaji.be.meeting.repository.MeetingEntity;
import com.haeyaji.be.meeting.repository.MeetingParticipantEntity;
import com.haeyaji.be.meeting.repository.MeetingParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingParticipationService {

    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingFinder meetingFinder;
    private final Clock clock;

    /** 공유 URL로 합류 (MEET-4). 이미 참여 중이면 기존 행을 그대로 반환한다(멱등). */
    @Transactional
    public MeetingParticipant join(String shareToken, UUID memberId) {
        MeetingEntity meeting = meetingFinder.getCollecting(shareToken, LocalDateTime.now(clock));
        return meetingParticipantRepository
                .findByMeetingIdAndMemberId(meeting.getId(), memberId)
                .orElseGet(() -> meetingParticipantRepository.save(
                        MeetingParticipantEntity.create(meeting.getId(), memberId)))
                .toDomain();
    }
}
