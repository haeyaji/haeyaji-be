package com.haeyaji.be.meeting.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MeetingTimeSlotRepository extends JpaRepository<MeetingTimeSlotEntity, UUID> {

    List<MeetingTimeSlotEntity> findByMeetingIdOrderBySlotStartAt(UUID meetingId);

    /** 약속 수정(시간 칸 재생성)·삭제 시 일괄 정리. 응답을 먼저 지운 뒤 호출해야 한다. */
    void deleteByMeetingId(UUID meetingId);
}
