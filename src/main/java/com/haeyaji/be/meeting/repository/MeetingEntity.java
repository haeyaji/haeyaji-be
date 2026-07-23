package com.haeyaji.be.meeting.repository;

import com.haeyaji.be.common.jpa.ImmutableBaseEntity;
import com.haeyaji.be.meeting.domain.Meeting;
import com.haeyaji.be.meeting.domain.MeetingStatus;
import com.haeyaji.be.meeting.domain.MeetingType;
import com.haeyaji.be.meeting.domain.TimeGrid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * 약속 테이블(meeting) 매핑. 비즈니스 로직은 여기 두지 않고 {@code domain.Meeting}으로 변환해 넘긴다.
 * id/createdAt은 {@link ImmutableBaseEntity}에서 상속 (meeting은 updated_at 없음).
 */
@Entity
@Table(name = "meeting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingEntity extends ImmutableBaseEntity {

    @Column(name = "creator_id", nullable = false)
    private UUID creatorId;

    @Column(nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeetingType type;

    @Column(name = "time_start", nullable = false)
    private LocalTime timeStart;

    @Column(name = "time_end", nullable = false)
    private LocalTime timeEnd;

    @Column(name = "slot_unit_minutes", nullable = false)
    private int slotUnitMinutes;

    private LocalDateTime deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeetingStatus status;

    @Column(name = "confirmed_start_at")
    private LocalDateTime confirmedStartAt;

    @Column(name = "confirmed_end_at")
    private LocalDateTime confirmedEndAt;

    @Column(name = "share_token", nullable = false, length = 64, unique = true)
    private String shareToken;

    public static MeetingEntity create(UUID creatorId, String title, MeetingType type,
            TimeGrid grid, LocalDateTime deadline, String shareToken) {
        MeetingEntity entity = new MeetingEntity();
        entity.creatorId = creatorId;
        entity.title = title;
        entity.type = type;
        entity.timeStart = grid.timeStart();
        entity.timeEnd = grid.timeEnd();
        entity.slotUnitMinutes = grid.slotUnitMinutes();
        entity.deadline = deadline;
        entity.status = MeetingStatus.COLLECTING;
        entity.shareToken = shareToken;
        return entity;
    }

    public void confirm(LocalDateTime startAt, LocalDateTime endAt) {
        this.confirmedStartAt = startAt;
        this.confirmedEndAt = endAt;
        this.status = MeetingStatus.CONFIRMED;
    }

    public Meeting toDomain() {
        return Meeting.builder()
                .id(getId())
                .creatorId(creatorId)
                .title(title)
                .type(type)
                .timeStart(timeStart)
                .timeEnd(timeEnd)
                .slotUnitMinutes(slotUnitMinutes)
                .deadline(deadline)
                .status(status)
                .confirmedStartAt(confirmedStartAt)
                .confirmedEndAt(confirmedEndAt)
                .shareToken(shareToken)
                .createdAt(getCreatedAt())
                .build();
    }
}
