package com.haeyaji.be.meeting.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MeetingDateRepository extends JpaRepository<MeetingDateEntity, UUID> {

    List<MeetingDateEntity> findByMeetingIdOrderByCandidateDate(UUID meetingId);
}
