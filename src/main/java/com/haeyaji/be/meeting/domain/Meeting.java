package com.haeyaji.be.meeting.domain;

import lombok.Builder;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * 약속 도메인 모델. DB 매핑({@code repository.MeetingEntity})과 분리된 순수 객체.
 * 만료(EXPIRED)는 영속 전이 없이 {@link #resolveStatus(LocalDateTime)}로 조회 시점에 판정한다 (MEET-12).
 */
@Builder(toBuilder = true)
public record Meeting(
        UUID id,
        UUID creatorId,
        String title,
        MeetingType type,
        LocalTime timeStart,
        LocalTime timeEnd,
        int slotUnitMinutes,
        LocalDateTime deadline,
        MeetingStatus status,
        LocalDateTime confirmedStartAt,
        LocalDateTime confirmedEndAt,
        String shareToken,
        LocalDateTime createdAt
) {

    /** 마감이 지난 COLLECTING 약속은 EXPIRED로 판정. CONFIRMED는 만료되지 않는다. */
    public MeetingStatus statusAt(LocalDateTime now) {
        if (status == MeetingStatus.COLLECTING && deadline != null && now.isAfter(deadline)) {
            return MeetingStatus.EXPIRED;
        }
        return status;
    }

    /** 조회 시점 유효 상태를 반영한 사본. */
    public Meeting resolveStatus(LocalDateTime now) {
        MeetingStatus effective = statusAt(now);
        return effective == status ? this : toBuilder().status(effective).build();
    }

    public boolean isCreator(UUID memberId) {
        return creatorId.equals(memberId);
    }

    public TimeGrid timeGrid() {
        return new TimeGrid(timeStart, timeEnd, slotUnitMinutes);
    }
}
