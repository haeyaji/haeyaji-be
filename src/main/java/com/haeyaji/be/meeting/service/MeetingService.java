package com.haeyaji.be.meeting.service;

import com.haeyaji.be.common.exception.BusinessException;
import com.haeyaji.be.meeting.domain.CandidateDates;
import com.haeyaji.be.meeting.domain.Meeting;
import com.haeyaji.be.meeting.domain.MeetingConfirmedEvent;
import com.haeyaji.be.meeting.domain.MeetingDetail;
import com.haeyaji.be.meeting.domain.MeetingErrorCode;
import com.haeyaji.be.meeting.domain.MeetingParticipant;
import com.haeyaji.be.meeting.domain.MeetingSummary;
import com.haeyaji.be.meeting.domain.ShareTokenGenerator;
import com.haeyaji.be.meeting.domain.TimeGrid;
import com.haeyaji.be.meeting.dto.MeetingConfirmRequest;
import com.haeyaji.be.meeting.dto.MeetingCreateRequest;
import com.haeyaji.be.meeting.repository.MeetingDateEntity;
import com.haeyaji.be.meeting.repository.MeetingDateRepository;
import com.haeyaji.be.meeting.repository.MeetingEntity;
import com.haeyaji.be.meeting.repository.MeetingParticipantCount;
import com.haeyaji.be.meeting.repository.MeetingParticipantEntity;
import com.haeyaji.be.meeting.repository.MeetingParticipantRepository;
import com.haeyaji.be.meeting.repository.MeetingRepository;
import com.haeyaji.be.meeting.repository.MeetingTimeSlotEntity;
import com.haeyaji.be.meeting.repository.MeetingTimeSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingDateRepository meetingDateRepository;
    private final MeetingTimeSlotRepository meetingTimeSlotRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingFinder meetingFinder;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public MeetingDetail create(UUID creatorId, MeetingCreateRequest request) {
        CandidateDates candidateDates = CandidateDates.of(request.dates(), LocalDate.now(clock));
        TimeGrid grid = TimeGrid.of(request.timeStart(), request.timeEnd(), request.slotUnitMinutes());

        MeetingEntity meeting = meetingRepository.save(MeetingEntity.create(
                creatorId, request.title(), request.type(),
                grid, request.deadline(), ShareTokenGenerator.generate()));
        UUID meetingId = meeting.getId();

        meetingDateRepository.saveAll(candidateDates.dates().stream()
                .map(date -> MeetingDateEntity.create(meetingId, date))
                .toList());
        meetingTimeSlotRepository.saveAll(grid.expandSlotStarts(candidateDates.dates()).stream()
                .map(slotStart -> MeetingTimeSlotEntity.create(meetingId, slotStart))
                .toList());
        meetingParticipantRepository.save(MeetingParticipantEntity.create(meetingId, creatorId));

        return loadDetail(meeting);
    }

    public MeetingDetail getByShareToken(String shareToken) {
        return loadDetail(meetingFinder.getByShareToken(shareToken));
    }

    public List<MeetingSummary> listByMember(UUID memberId) {
        List<MeetingEntity> meetings = meetingRepository.findAllByMember(memberId);
        List<UUID> meetingIds = meetings.stream().map(MeetingEntity::getId).toList();
        Map<UUID, Long> participantCounts = meetingIds.isEmpty()
                ? Map.of()
                : meetingParticipantRepository.countByMeetingIds(meetingIds).stream()
                        .collect(Collectors.toMap(MeetingParticipantCount::meetingId, MeetingParticipantCount::count));
        LocalDateTime now = LocalDateTime.now(clock);
        return meetings.stream()
                .map(entity -> new MeetingSummary(
                        entity.toDomain().resolveStatus(now),
                        participantCounts.getOrDefault(entity.getId(), 0L)))
                .toList();
    }

    @Transactional
    public MeetingDetail confirm(String shareToken, UUID memberId, MeetingConfirmRequest request) {
        MeetingEntity entity = meetingFinder.getCollecting(shareToken, LocalDateTime.now(clock));
        Meeting meeting = entity.toDomain();
        if (!meeting.isCreator(memberId)) {
            throw new BusinessException(MeetingErrorCode.NOT_MEETING_CREATOR);
        }
        validateConfirmRange(meeting, request.confirmedStartAt(), request.confirmedEndAt());
        entity.confirm(request.confirmedStartAt(), request.confirmedEndAt());
        MeetingDetail detail = loadDetail(entity);

        // 알림(noti) 연계 지점 — 참여자 전원에게 확정 알림을 만들 수 있도록 이벤트 발행
        eventPublisher.publishEvent(new MeetingConfirmedEvent(
                entity.getId(), entity.getShareToken(), entity.getTitle(),
                request.confirmedStartAt(), request.confirmedEndAt(),
                detail.participants().stream().map(MeetingParticipant::memberId).toList()));
        return detail;
    }

    /** 확정 범위 규칙: start < end, 양끝 그리드 정렬, 구간 내 모든 슬롯이 실제 후보 칸으로 존재(연속 보장). */
    private void validateConfirmRange(Meeting meeting, LocalDateTime startAt, LocalDateTime endAt) {
        TimeGrid grid = meeting.timeGrid();
        if (!startAt.isBefore(endAt) || !grid.isAligned(startAt) || !grid.isAligned(endAt)) {
            throw new BusinessException(MeetingErrorCode.INVALID_CONFIRM_RANGE);
        }
        Set<LocalDateTime> slotStarts = meetingTimeSlotRepository
                .findByMeetingIdOrderBySlotStartAt(meeting.id()).stream()
                .map(MeetingTimeSlotEntity::getSlotStartAt)
                .collect(Collectors.toCollection(HashSet::new));
        boolean allWithinSlots = grid.slotStartsBetween(startAt, endAt).stream()
                .allMatch(slotStarts::contains);
        if (!allWithinSlots) {
            throw new BusinessException(MeetingErrorCode.INVALID_CONFIRM_RANGE);
        }
    }

    private MeetingDetail loadDetail(MeetingEntity entity) {
        UUID meetingId = entity.getId();
        return new MeetingDetail(
                entity.toDomain().resolveStatus(LocalDateTime.now(clock)),
                meetingDateRepository.findByMeetingIdOrderByCandidateDate(meetingId).stream()
                        .map(MeetingDateEntity::getCandidateDate)
                        .toList(),
                meetingTimeSlotRepository.findByMeetingIdOrderBySlotStartAt(meetingId).stream()
                        .map(MeetingTimeSlotEntity::toDomain)
                        .toList(),
                meetingParticipantRepository.findByMeetingIdOrderByJoinedAt(meetingId).stream()
                        .map(MeetingParticipantEntity::toDomain)
                        .toList());
    }
}
