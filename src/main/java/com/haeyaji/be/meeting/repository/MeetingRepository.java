package com.haeyaji.be.meeting.repository;

import com.haeyaji.be.meeting.domain.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeetingRepository extends JpaRepository<MeetingEntity, UUID> {

    Optional<MeetingEntity> findByShareToken(String shareToken);

    /** MEETING_REMINDER 스케줄러용 — 확정 상태이고 시작 시각이 정확히 일치하는 약속 조회. */
    List<MeetingEntity> findByStatusAndConfirmedStartAt(MeetingStatus status, LocalDateTime confirmedStartAt);

    /** 생성했거나 참여 중인 약속 목록 (MEET-11). */
    @Query("""
            select m from MeetingEntity m
            where m.creatorId = :memberId
               or m.id in (select p.meetingId from MeetingParticipantEntity p where p.memberId = :memberId)
            order by m.createdAt desc""")
    List<MeetingEntity> findAllByMember(@Param("memberId") UUID memberId);
}
