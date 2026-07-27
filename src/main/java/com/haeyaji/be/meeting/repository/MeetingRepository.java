package com.haeyaji.be.meeting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeetingRepository extends JpaRepository<MeetingEntity, UUID> {

    Optional<MeetingEntity> findByShareToken(String shareToken);

    /** 확정 시각이 코앞인 약속 — 리마인더 대상. 확정 전(COLLECTING)엔 알릴 시각 자체가 없다. */
    List<MeetingEntity> findByStatusAndConfirmedStartAtBetween(
            com.haeyaji.be.meeting.domain.MeetingStatus status,
            java.time.LocalDateTime from, java.time.LocalDateTime to);

    /** 생성했거나 참여 중인 약속 목록 (MEET-11). */
    @Query("""
            select m from MeetingEntity m
            where m.creatorId = :memberId
               or m.id in (select p.meetingId from MeetingParticipantEntity p where p.memberId = :memberId)
            order by m.createdAt desc""")
    List<MeetingEntity> findAllByMember(@Param("memberId") UUID memberId);
}
