package com.haeyaji.be.meeting.repository;

import com.haeyaji.be.common.jpa.UuidBaseEntity;
import com.haeyaji.be.meeting.domain.MeetingParticipant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 참여자 명단 테이블(meeting_participant) 매핑. 스키마 컬럼이 created_at이 아닌 joined_at이라
 * {@link UuidBaseEntity} 위에 {@code @CreatedDate}를 직접 선언한다.
 */
@Entity
@Table(name = "meeting_participant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingParticipantEntity extends UuidBaseEntity {

    @Column(name = "meeting_id", nullable = false)
    private UUID meetingId;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @CreatedDate
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    public static MeetingParticipantEntity create(UUID meetingId, UUID memberId) {
        MeetingParticipantEntity entity = new MeetingParticipantEntity();
        entity.meetingId = meetingId;
        entity.memberId = memberId;
        return entity;
    }

    public MeetingParticipant toDomain() {
        return new MeetingParticipant(getId(), meetingId, memberId, joinedAt);
    }
}
