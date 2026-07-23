package com.haeyaji.be.meeting.service;

import com.haeyaji.be.common.exception.BusinessException;
import com.haeyaji.be.meeting.domain.MeetingAvailability;
import com.haeyaji.be.meeting.domain.MeetingErrorCode;
import com.haeyaji.be.meeting.domain.MeetingParticipant;
import com.haeyaji.be.meeting.domain.MeetingSlot;
import com.haeyaji.be.meeting.domain.ParticipantResponses;
import com.haeyaji.be.meeting.domain.ResponseBoard;
import com.haeyaji.be.meeting.domain.SlotResponse;
import com.haeyaji.be.meeting.domain.TimeWindow;
import com.haeyaji.be.meeting.dto.ResponseSubmitRequest;
import com.haeyaji.be.meeting.dto.SlotResponseItem;
import com.haeyaji.be.meeting.repository.MeetingEntity;
import com.haeyaji.be.meeting.repository.MeetingParticipantEntity;
import com.haeyaji.be.meeting.repository.MeetingParticipantRepository;
import com.haeyaji.be.meeting.repository.MeetingResponseEntity;
import com.haeyaji.be.meeting.repository.MeetingResponseRepository;
import com.haeyaji.be.meeting.repository.MeetingTimeSlotEntity;
import com.haeyaji.be.meeting.repository.MeetingTimeSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingResponseService {

    private final MeetingTimeSlotRepository meetingTimeSlotRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingResponseRepository meetingResponseRepository;
    private final MeetingFinder meetingFinder;
    private final Clock clock;

    /**
     * 가능 시간 제출·재수정 (MEET-5). PUT full-replace: payload에 있는 칸은 갱신/생성,
     * 없는 칸의 기존 응답은 삭제한다.
     */
    @Transactional
    public List<SlotResponse> submit(String shareToken, UUID memberId, ResponseSubmitRequest request) {
        MeetingEntity meeting = meetingFinder.getCollecting(shareToken, LocalDateTime.now(clock));
        if (!meetingParticipantRepository.existsByMeetingIdAndMemberId(meeting.getId(), memberId)) {
            throw new BusinessException(MeetingErrorCode.NOT_MEETING_PARTICIPANT);
        }

        Set<UUID> meetingSlotIds = meetingTimeSlotRepository
                .findByMeetingIdOrderBySlotStartAt(meeting.getId()).stream()
                .map(MeetingTimeSlotEntity::getId)
                .collect(Collectors.toSet());
        List<UUID> requestedSlotIds = request.responses().stream().map(SlotResponseItem::slotId).toList();
        boolean hasDuplicate = Set.copyOf(requestedSlotIds).size() != requestedSlotIds.size();
        if (hasDuplicate || !meetingSlotIds.containsAll(requestedSlotIds)) {
            throw new BusinessException(MeetingErrorCode.INVALID_MEETING_SLOT);
        }

        Map<UUID, MeetingResponseEntity> existingBySlot = meetingResponseRepository
                .findByMemberIdAndMeetingTimeSlotIdIn(memberId, meetingSlotIds).stream()
                .collect(Collectors.toMap(MeetingResponseEntity::getMeetingTimeSlotId, Function.identity()));

        List<MeetingResponseEntity> saved = new ArrayList<>();
        for (SlotResponseItem item : request.responses()) {
            MeetingResponseEntity existing = existingBySlot.remove(item.slotId());
            if (existing != null) {
                existing.changeStatus(item.status());
                saved.add(existing);
            } else {
                saved.add(meetingResponseRepository.save(
                        MeetingResponseEntity.create(item.slotId(), memberId, item.status())));
            }
        }
        meetingResponseRepository.deleteAll(existingBySlot.values());

        return saved.stream().map(MeetingResponseEntity::toDomain).toList();
    }

    /** 슬롯별 가능 인원 집계 (MEET-6). 확정·만료 후에도 조회 가능. */
    public MeetingAvailability getAvailability(String shareToken) {
        MeetingEntity meeting = meetingFinder.getByShareToken(shareToken);
        return buildAvailability(meeting);
    }

    /** 참여 인원이 최대인 최적 연속 구간 (MEET-7). */
    public List<TimeWindow> getBestWindows(String shareToken) {
        MeetingEntity meeting = meetingFinder.getByShareToken(shareToken);
        return buildAvailability(meeting).bestWindows(meeting.getSlotUnitMinutes());
    }

    /** 참여자별 응답 현황 (MEET-8). */
    public ResponseBoard getResponseBoard(String shareToken) {
        MeetingEntity meeting = meetingFinder.getByShareToken(shareToken);
        List<MeetingSlot> slots = loadSlots(meeting.getId());
        Map<UUID, LocalDateTime> slotStartById = slots.stream()
                .collect(Collectors.toMap(MeetingSlot::id, MeetingSlot::slotStartAt));
        Map<UUID, List<SlotResponse>> responsesByMember = loadResponses(slotStartById.keySet()).stream()
                .collect(Collectors.groupingBy(SlotResponse::memberId));

        List<ParticipantResponses> participants = meetingParticipantRepository
                .findByMeetingIdOrderByJoinedAt(meeting.getId()).stream()
                .map(MeetingParticipantEntity::toDomain)
                .map(participant -> new ParticipantResponses(
                        participant,
                        responsesByMember.getOrDefault(participant.memberId(), List.of()).stream()
                                .sorted(Comparator.comparing(response -> slotStartById.get(response.slotId())))
                                .toList()))
                .toList();
        return new ResponseBoard(participants.size(), participants);
    }

    private MeetingAvailability buildAvailability(MeetingEntity meeting) {
        List<MeetingSlot> slots = loadSlots(meeting.getId());
        List<SlotResponse> responses = loadResponses(
                slots.stream().map(MeetingSlot::id).collect(Collectors.toSet()));
        long participantCount = meetingParticipantRepository.countByMeetingId(meeting.getId());
        return MeetingAvailability.of(slots, responses, Math.toIntExact(participantCount));
    }

    private List<MeetingSlot> loadSlots(UUID meetingId) {
        return meetingTimeSlotRepository.findByMeetingIdOrderBySlotStartAt(meetingId).stream()
                .map(MeetingTimeSlotEntity::toDomain)
                .toList();
    }

    private List<SlotResponse> loadResponses(Set<UUID> slotIds) {
        if (slotIds.isEmpty()) {
            return List.of();
        }
        return meetingResponseRepository.findByMeetingTimeSlotIdIn(slotIds).stream()
                .map(MeetingResponseEntity::toDomain)
                .toList();
    }
}
