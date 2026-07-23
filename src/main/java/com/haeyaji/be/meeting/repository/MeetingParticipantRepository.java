package com.haeyaji.be.meeting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipantEntity, UUID> {

    Optional<MeetingParticipantEntity> findByMeetingIdAndMemberId(UUID meetingId, UUID memberId);

    boolean existsByMeetingIdAndMemberId(UUID meetingId, UUID memberId);

    List<MeetingParticipantEntity> findByMeetingIdOrderByJoinedAt(UUID meetingId);

    long countByMeetingId(UUID meetingId);

    /** 목록 조회용 약속별 참여 인원 수. */
    @Query("""
            select new com.haeyaji.be.meeting.repository.MeetingParticipantCount(p.meetingId, count(p))
            from MeetingParticipantEntity p
            where p.meetingId in :meetingIds
            group by p.meetingId""")
    List<MeetingParticipantCount> countByMeetingIds(@Param("meetingIds") Collection<UUID> meetingIds);
}
