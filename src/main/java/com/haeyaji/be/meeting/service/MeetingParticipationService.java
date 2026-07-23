package com.haeyaji.be.meeting.service;

import com.haeyaji.be.common.exception.BusinessException;
import com.haeyaji.be.meeting.domain.MeetingErrorCode;
import com.haeyaji.be.meeting.domain.MeetingInviteResult;
import com.haeyaji.be.meeting.domain.MeetingInvitedEvent;
import com.haeyaji.be.meeting.domain.MeetingParticipant;
import com.haeyaji.be.meeting.repository.MeetingEntity;
import com.haeyaji.be.meeting.repository.MeetingParticipantEntity;
import com.haeyaji.be.meeting.repository.MeetingParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingParticipationService {

    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingFinder meetingFinder;
    private final ApplicationEventPublisher eventPublisher;
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

    /**
     * 약속 초대 — 알림(noti) 연계 지점. 초대 행을 저장하지 않고 {@link MeetingInvitedEvent}만 발행한다.
     * 중복 memberId는 1건으로 처리하고, 이미 참여 중인 회원은 스킵한다. 초대자는 참여자여야 한다.
     */
    public MeetingInviteResult invite(String shareToken, UUID inviterId, List<UUID> memberIds) {
        MeetingEntity meeting = meetingFinder.getCollecting(shareToken, LocalDateTime.now(clock));
        if (!meetingParticipantRepository.existsByMeetingIdAndMemberId(meeting.getId(), inviterId)) {
            throw new BusinessException(MeetingErrorCode.NOT_MEETING_PARTICIPANT);
        }
        List<UUID> targets = memberIds.stream().distinct().toList();
        Set<UUID> alreadyJoined = meetingParticipantRepository
                .findByMeetingIdAndMemberIdIn(meeting.getId(), targets).stream()
                .map(MeetingParticipantEntity::getMemberId)
                .collect(Collectors.toSet());
        List<UUID> invitees = targets.stream().filter(id -> !alreadyJoined.contains(id)).toList();
        List<UUID> skipped = targets.stream().filter(alreadyJoined::contains).toList();

        if (!invitees.isEmpty()) {
            eventPublisher.publishEvent(new MeetingInvitedEvent(
                    meeting.getId(), meeting.getShareToken(), meeting.getTitle(), inviterId, invitees));
        }
        return new MeetingInviteResult(invitees, skipped);
    }
}
