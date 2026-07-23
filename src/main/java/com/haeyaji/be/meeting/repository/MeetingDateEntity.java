package com.haeyaji.be.meeting.repository;

import com.haeyaji.be.common.jpa.UuidBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 후보 날짜 테이블(meeting_date) 매핑. 범위가 아닌 개별 날짜라 산발 선택(월·수·금) 가능.
 */
@Entity
@Table(name = "meeting_date")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingDateEntity extends UuidBaseEntity {

    @Column(name = "meeting_id", nullable = false)
    private UUID meetingId;

    @Column(name = "candidate_date", nullable = false)
    private LocalDate candidateDate;

    public static MeetingDateEntity create(UUID meetingId, LocalDate candidateDate) {
        MeetingDateEntity entity = new MeetingDateEntity();
        entity.meetingId = meetingId;
        entity.candidateDate = candidateDate;
        return entity;
    }
}
