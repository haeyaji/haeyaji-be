package com.haeyaji.be.meeting.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MeetingTimeSlotRepository extends JpaRepository<MeetingTimeSlotEntity, UUID> {

    List<MeetingTimeSlotEntity> findByMeetingIdOrderBySlotStartAt(UUID meetingId);
}
