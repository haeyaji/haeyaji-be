package com.haeyaji.be.meeting.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MeetingResponseRepository extends JpaRepository<MeetingResponseEntity, UUID> {

    List<MeetingResponseEntity> findByMeetingTimeSlotIdIn(Collection<UUID> slotIds);

    List<MeetingResponseEntity> findByMemberIdAndMeetingTimeSlotIdIn(UUID memberId, Collection<UUID> slotIds);
}
