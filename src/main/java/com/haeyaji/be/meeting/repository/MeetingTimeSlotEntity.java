package com.haeyaji.be.meeting.repository;

import com.haeyaji.be.common.jpa.UuidBaseEntity;
import com.haeyaji.be.meeting.domain.MeetingSlot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 투표 칸 테이블(meeting_time_slot) 매핑. (후보 날짜 × 시간 범위 / 단위)로 생성 시점에 일괄 저장된다.
 */
@Entity
@Table(name = "meeting_time_slot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingTimeSlotEntity extends UuidBaseEntity {

    @Column(name = "meeting_id", nullable = false)
    private UUID meetingId;

    @Column(name = "slot_start_at", nullable = false)
    private LocalDateTime slotStartAt;

    public static MeetingTimeSlotEntity create(UUID meetingId, LocalDateTime slotStartAt) {
        MeetingTimeSlotEntity entity = new MeetingTimeSlotEntity();
        entity.meetingId = meetingId;
        entity.slotStartAt = slotStartAt;
        return entity;
    }

    public MeetingSlot toDomain() {
        return new MeetingSlot(getId(), slotStartAt);
    }
}
