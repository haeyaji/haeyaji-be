package com.haeyaji.be.meeting.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MeetingDateRepository extends JpaRepository<MeetingDateEntity, UUID> {

    List<MeetingDateEntity> findByMeetingIdOrderByCandidateDate(UUID meetingId);

    /** 약속 수정(시간 칸 재생성)·삭제 시 일괄 정리. */
    void deleteByMeetingId(UUID meetingId);
}
