package com.haeyaji.be.meeting.service;

import com.haeyaji.be.common.exception.BusinessException;
import com.haeyaji.be.meeting.domain.CandidateDates;
import com.haeyaji.be.meeting.domain.InviteStatus;
import com.haeyaji.be.meeting.domain.Meeting;
import com.haeyaji.be.meeting.domain.MeetingConfirmedEvent;
import com.haeyaji.be.meeting.domain.MeetingDeletedEvent;
import com.haeyaji.be.meeting.domain.MeetingDetail;
import com.haeyaji.be.meeting.domain.MeetingErrorCode;
import com.haeyaji.be.meeting.domain.MeetingParticipant;
import com.haeyaji.be.meeting.domain.MeetingSummary;
import com.haeyaji.be.meeting.domain.ShareTokenGenerator;
import com.haeyaji.be.meeting.domain.TimeGrid;
import com.haeyaji.be.meeting.dto.MeetingConfirmRequest;
import com.haeyaji.be.meeting.dto.MeetingCreateRequest;
import com.haeyaji.be.meeting.dto.MeetingUpdateRequest;
import com.haeyaji.be.meeting.repository.MeetingDateEntity;
import com.haeyaji.be.meeting.repository.MeetingDateRepository;
import com.haeyaji.be.meeting.repository.MeetingEntity;
import com.haeyaji.be.meeting.repository.MeetingParticipantCount;
import com.haeyaji.be.meeting.repository.MeetingParticipantEntity;
import com.haeyaji.be.meeting.repository.MeetingParticipantRepository;
import com.haeyaji.be.meeting.repository.MeetingRepository;
import com.haeyaji.be.meeting.repository.MeetingResponseRepository;
import com.haeyaji.be.meeting.repository.MeetingTimeSlotEntity;
import com.haeyaji.be.meeting.repository.MeetingTimeSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
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
    private final MeetingResponseRepository meetingResponseRepository;
    private final MeetingFinder meetingFinder;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /** 응답 마감 기본값 — 지정하지 않으면 생성 시각으로부터 2시간. 이때까지 모인 응답으로 집계가 공개된다. */
    private static final Duration DEFAULT_RESPONSE_WINDOW = Duration.ofHours(2);

    @Transactional
    public MeetingDetail create(UUID creatorId, MeetingCreateRequest request) {
        CandidateDates candidateDates = CandidateDates.of(request.dates(), LocalDate.now(clock));
        TimeGrid grid = TimeGrid.of(request.timeStart(), request.timeEnd(), request.slotUnitMinutes());

        LocalDateTime deadline = request.deadline() != null
                ? request.deadline()
                : LocalDateTime.now(clock).plus(DEFAULT_RESPONSE_WINDOW);
        MeetingEntity meeting = meetingRepository.save(MeetingEntity.create(
                creatorId, request.title(), request.type(),
                grid, deadline, ShareTokenGenerator.generate()));
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
                : meetingParticipantRepository.countByMeetingIds(meetingIds, InviteStatus.ACCEPTED).stream()
                        .collect(Collectors.toMap(MeetingParticipantCount::meetingId, MeetingParticipantCount::count));
        LocalDateTime now = LocalDateTime.now(clock);
        return meetings.stream()
                .map(entity -> new MeetingSummary(
                        entity.toDomain().resolveStatus(now),
                        participantCounts.getOrDefault(entity.getId(), 0L)))
                .toList();
    }

    /**
     * 약속 수정 (MEET-15). <b>방장만</b>, <b>아직 아무도 응답하지 않았을 때만</b> 가능하다.
     *
     * <p>응답이 하나라도 들어오면 잠그는 이유: 시간 칸을 다시 깔면 남이 골라둔 가능 시간이 통째로
     * 사라진다. 고른 사람은 그 사실을 알 방법이 없어 "분명 체크했는데 비어 있다"가 된다.
     * 확정 후에도 잠긴다 — 이미 캘린더에 퍼진 일정이라 되돌리려면 삭제가 맞다.
     */
    @Transactional
    public MeetingDetail update(String shareToken, UUID memberId, MeetingUpdateRequest request) {
        MeetingEntity entity = meetingFinder.getUnconfirmed(shareToken);
        if (!entity.toDomain().isCreator(memberId)) {
            throw new BusinessException(MeetingErrorCode.NOT_MEETING_CREATOR);
        }
        List<UUID> slotIds = meetingTimeSlotRepository.findByMeetingIdOrderBySlotStartAt(entity.getId()).stream()
                .map(MeetingTimeSlotEntity::getId)
                .toList();
        if (!slotIds.isEmpty() && !meetingResponseRepository.findByMeetingTimeSlotIdIn(slotIds).isEmpty()) {
            throw new BusinessException(MeetingErrorCode.MEETING_ALREADY_RESPONDED);
        }

        TimeGrid grid = null;
        if (request.touchesTimeGrid()) {
            if (!request.hasCompleteTimeGrid()) {
                throw new BusinessException(MeetingErrorCode.INCOMPLETE_TIME_GRID);
            }
            grid = TimeGrid.of(request.timeStart(), request.timeEnd(), request.slotUnitMinutes());
            rebuildTimeGrid(entity.getId(), CandidateDates.of(request.dates(), LocalDate.now(clock)), grid);
        }
        entity.update(request.title(), grid, request.deadline());
        return loadDetail(entity);
    }

    /** 후보 날짜·시간 칸을 갈아엎는다. 응답이 없을 때만 호출되므로 지울 응답도 없다. */
    private void rebuildTimeGrid(UUID meetingId, CandidateDates dates, TimeGrid grid) {
        meetingDateRepository.deleteByMeetingId(meetingId);
        meetingTimeSlotRepository.deleteByMeetingId(meetingId);
        meetingDateRepository.saveAll(dates.dates().stream()
                .map(date -> MeetingDateEntity.create(meetingId, date))
                .toList());
        meetingTimeSlotRepository.saveAll(grid.expandSlotStarts(dates.dates()).stream()
                .map(slotStart -> MeetingTimeSlotEntity.create(meetingId, slotStart))
                .toList());
    }

    /**
     * 약속 삭제 (MEET-16). 방장만. 확정 여부와 무관하게 지울 수 있다 —
     * 확정 후엔 수정이 막히므로 잘못 확정한 약속을 되돌릴 방법이 이것뿐이다.
     *
     * <p>약속에 딸린 행(응답·시간 칸·후보 날짜·참여자)은 여기서 지우고, 약속 <b>바깥</b>으로 퍼진 것
     * (공유 할 일·알림)은 {@link MeetingDeletedEvent}를 구독한 각 모듈이 정리한다.
     */
    @Transactional
    public void delete(String shareToken, UUID memberId) {
        MeetingEntity entity = meetingFinder.getByShareToken(shareToken);
        if (!entity.toDomain().isCreator(memberId)) {
            throw new BusinessException(MeetingErrorCode.NOT_MEETING_CREATOR);
        }
        UUID meetingId = entity.getId();

        List<UUID> slotIds = meetingTimeSlotRepository.findByMeetingIdOrderBySlotStartAt(meetingId).stream()
                .map(MeetingTimeSlotEntity::getId)
                .toList();
        if (!slotIds.isEmpty()) {
            // 응답은 슬롯을 참조한다 — 슬롯보다 먼저 지워야 참조가 끊긴 응답이 남지 않는다.
            meetingResponseRepository.deleteByMeetingTimeSlotIdIn(slotIds);
        }
        meetingTimeSlotRepository.deleteByMeetingId(meetingId);
        meetingDateRepository.deleteByMeetingId(meetingId);
        meetingParticipantRepository.deleteByMeetingId(meetingId);
        meetingRepository.delete(entity);

        eventPublisher.publishEvent(new MeetingDeletedEvent(meetingId, entity.getTitle()));
    }

    @Transactional
    public MeetingDetail confirm(String shareToken, UUID memberId, MeetingConfirmRequest request) {
        MeetingEntity entity = meetingFinder.getUnconfirmed(shareToken);
        Meeting meeting = entity.toDomain();
        if (!meeting.isCreator(memberId)) {
            throw new BusinessException(MeetingErrorCode.NOT_MEETING_CREATOR);
        }
        validateConfirmRange(meeting, request.confirmedStartAt(), request.confirmedEndAt());
        entity.confirm(request.confirmedStartAt(), request.confirmedEndAt());
        MeetingDetail detail = loadDetail(entity);

        // 확정 후속 처리 이벤트 — todo 모듈이 공유 할 일로 전환(MEET-10), 알림 모듈이 확정 알림 생성
        eventPublisher.publishEvent(new MeetingConfirmedEvent(
                entity.getId(), entity.getShareToken(), entity.getTitle(), meeting.creatorId(),
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
                meetingParticipantRepository.findByMeetingIdAndInviteStatusOrderByJoinedAt(meetingId, InviteStatus.ACCEPTED).stream()
                        .map(MeetingParticipantEntity::toDomain)
                        .toList());
    }
}
