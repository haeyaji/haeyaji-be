package com.haeyaji.be.meeting.repository;

import com.haeyaji.be.common.jpa.UuidBaseEntity;
import com.haeyaji.be.meeting.domain.InviteStatus;
import com.haeyaji.be.meeting.domain.MeetingParticipant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "invite_status", nullable = false)
    private InviteStatus inviteStatus;

    /** 링크로 직접 합류(또는 생성자 자동 참여) — 바로 ACCEPTED. */
    public static MeetingParticipantEntity create(UUID meetingId, UUID memberId) {
        MeetingParticipantEntity entity = new MeetingParticipantEntity();
        entity.meetingId = meetingId;
        entity.memberId = memberId;
        entity.inviteStatus = InviteStatus.ACCEPTED;
        return entity;
    }

    /** 초대받은 상태로 행을 남긴다 — 수락 전까지 집계·응답 권한에서 제외된다. */
    public static MeetingParticipantEntity invite(UUID meetingId, UUID memberId) {
        MeetingParticipantEntity entity = new MeetingParticipantEntity();
        entity.meetingId = meetingId;
        entity.memberId = memberId;
        entity.inviteStatus = InviteStatus.PENDING;
        return entity;
    }

    public void accept() {
        this.inviteStatus = InviteStatus.ACCEPTED;
    }

    /** 거절했던 사람을 다시 초대 — PENDING으로 되돌린다. */
    public void invite() {
        this.inviteStatus = InviteStatus.PENDING;
    }

    public void reject() {
        this.inviteStatus = InviteStatus.REJECTED;
    }

    public MeetingParticipant toDomain() {
        return new MeetingParticipant(getId(), meetingId, memberId, joinedAt, inviteStatus);
    }
}
