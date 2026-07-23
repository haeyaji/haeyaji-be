package com.haeyaji.be.meeting.repository;

import com.haeyaji.be.common.jpa.UuidBaseEntity;
import com.haeyaji.be.meeting.domain.ResponseStatus;
import com.haeyaji.be.meeting.domain.SlotResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 칸별 응답 테이블(meeting_response) 매핑. 스키마에 updated_at만 있어
 * {@link UuidBaseEntity} 위에 {@code @LastModifiedDate}를 직접 선언한다.
 */
@Entity
@Table(name = "meeting_response")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingResponseEntity extends UuidBaseEntity {

    @Column(name = "meeting_time_slot_id", nullable = false)
    private UUID meetingTimeSlotId;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ResponseStatus status;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static MeetingResponseEntity create(UUID meetingTimeSlotId, UUID memberId, ResponseStatus status) {
        MeetingResponseEntity entity = new MeetingResponseEntity();
        entity.meetingTimeSlotId = meetingTimeSlotId;
        entity.memberId = memberId;
        entity.status = status;
        return entity;
    }

    public void changeStatus(ResponseStatus status) {
        this.status = status;
    }

    public SlotResponse toDomain() {
        return new SlotResponse(getId(), meetingTimeSlotId, memberId, status);
    }
}
