package com.haeyaji.be.meeting.service;

import com.haeyaji.be.common.exception.BusinessException;
import com.haeyaji.be.common.exception.ErrorCode;
import com.haeyaji.be.meeting.domain.InviteStatus;
import com.haeyaji.be.meeting.domain.MeetingErrorCode;
import com.haeyaji.be.meeting.domain.MeetingInviteRespondedEvent;
import com.haeyaji.be.meeting.domain.MeetingInviteResult;
import com.haeyaji.be.meeting.domain.MeetingInvitedEvent;
import com.haeyaji.be.meeting.domain.MeetingParticipant;
import com.haeyaji.be.meeting.repository.MeetingEntity;
import com.haeyaji.be.meeting.repository.MeetingParticipantEntity;
import com.haeyaji.be.meeting.repository.MeetingParticipantRepository;
import com.haeyaji.be.meeting.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingParticipationService {

    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingFinder meetingFinder;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /**
     * 공유 URL로 합류 (MEET-4). 이미 수락 상태면 그대로 반환하고(멱등),
     * 초대만 받아둔(PENDING)·거절했던(REJECTED) 상태면 수락으로 전환한다.
     */
    @Transactional
    public MeetingParticipant join(String shareToken, UUID memberId) {
        MeetingEntity meeting = meetingFinder.getCollecting(shareToken, LocalDateTime.now(clock));
        MeetingParticipantEntity participant = meetingParticipantRepository
                .findByMeetingIdAndMemberId(meeting.getId(), memberId)
                .orElseGet(() -> meetingParticipantRepository.save(
                        MeetingParticipantEntity.create(meeting.getId(), memberId)));
        // 초대장에 응한 것인지, 링크로 그냥 들어온 것인지는 수락 전 상태로만 구분된다.
        boolean respondedToInvite = participant.getInviteStatus() == InviteStatus.PENDING;
        participant.accept();
        if (respondedToInvite) {
            publishInviteResponse(meeting, memberId, true);
        }
        return participant.toDomain();
    }

    private void publishInviteResponse(MeetingEntity meeting, UUID responderId, boolean accepted) {
        eventPublisher.publishEvent(new MeetingInviteRespondedEvent(
                meeting.getId(), meeting.getShareToken(), meeting.getTitle(),
                meeting.getCreatorId(), responderId, accepted));
    }

    /**
     * 약속 초대 (MEET-4). 초대를 <b>PENDING 행으로 남기고</b> {@link MeetingInvitedEvent}도 발행한다.
     * <p>행을 남기는 이유: 알림이 유실·삭제돼도 초대받은 사람이 {@link #getPendingInvitations}로 다시 찾을 수 있어야 한다
     * (할 일 공유 {@code todo_participant}와 같은 패턴). PENDING은 수락 전까지 참여 인원·응답 권한에서 제외된다.
     * <p>중복 memberId는 1건으로 처리하고, 이미 수락한 회원은 스킵한다. 초대자는 수락한 참여자여야 한다.
     */
    @Transactional
    public MeetingInviteResult invite(String shareToken, UUID inviterId, List<UUID> memberIds) {
        MeetingEntity meeting = meetingFinder.getCollecting(shareToken, LocalDateTime.now(clock));
        if (!meetingParticipantRepository.existsByMeetingIdAndMemberIdAndInviteStatus(
                meeting.getId(), inviterId, InviteStatus.ACCEPTED)) {
            throw new BusinessException(MeetingErrorCode.NOT_MEETING_PARTICIPANT);
        }
        List<UUID> targets = memberIds.stream().distinct().toList();
        Map<UUID, MeetingParticipantEntity> existing = meetingParticipantRepository
                .findByMeetingIdAndMemberIdIn(meeting.getId(), targets).stream()
                .collect(Collectors.toMap(MeetingParticipantEntity::getMemberId, Function.identity()));

        Set<UUID> alreadyJoined = existing.values().stream()
                .filter(p -> p.getInviteStatus() == InviteStatus.ACCEPTED)
                .map(MeetingParticipantEntity::getMemberId)
                .collect(Collectors.toSet());
        List<UUID> invitees = targets.stream().filter(id -> !alreadyJoined.contains(id)).toList();
        List<UUID> skipped = targets.stream().filter(alreadyJoined::contains).toList();

        for (UUID inviteeId : invitees) {
            MeetingParticipantEntity prior = existing.get(inviteeId);
            if (prior == null) {
                meetingParticipantRepository.save(MeetingParticipantEntity.invite(meeting.getId(), inviteeId));
            } else {
                prior.invite(); // 거절했던 사람 재초대 → 다시 PENDING
            }
        }
        if (!invitees.isEmpty()) {
            eventPublisher.publishEvent(new MeetingInvitedEvent(
                    meeting.getId(), meeting.getShareToken(), meeting.getTitle(), inviterId, invitees));
        }
        return new MeetingInviteResult(invitees, skipped);
    }

    /** 내가 받은 대기(PENDING) 약속 초대 목록 — 알림 없이도 초대를 찾을 수 있는 경로. */
    public List<MeetingInvitation> getPendingInvitations(UUID memberId) {
        List<MeetingParticipantEntity> pending = meetingParticipantRepository
                .findByMemberIdAndInviteStatus(memberId, InviteStatus.PENDING);
        if (pending.isEmpty()) {
            return List.of();
        }
        Map<UUID, MeetingEntity> meetings = meetingRepository
                .findAllById(pending.stream().map(MeetingParticipantEntity::getMeetingId).toList()).stream()
                .collect(Collectors.toMap(MeetingEntity::getId, Function.identity()));
        return pending.stream()
                .map(p -> meetings.get(p.getMeetingId()))
                .filter(java.util.Objects::nonNull)
                .map(m -> new MeetingInvitation(m.toDomain(), m.getShareToken()))
                .toList();
    }

    /** 받은 초대 거절. 거절해도 행은 남아 재초대가 가능하다. */
    @Transactional
    public void rejectInvitation(String shareToken, UUID memberId) {
        MeetingEntity meeting = meetingFinder.getCollecting(shareToken, LocalDateTime.now(clock));
        MeetingParticipantEntity participant = meetingParticipantRepository
                .findByMeetingIdAndMemberId(meeting.getId(), memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (participant.getInviteStatus() != InviteStatus.PENDING) {
            throw new BusinessException(MeetingErrorCode.ALREADY_RESPONDED_INVITATION);
        }
        participant.reject();
        publishInviteResponse(meeting, memberId, false);
    }
}
