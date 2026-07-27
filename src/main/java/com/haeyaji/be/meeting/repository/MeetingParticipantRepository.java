package com.haeyaji.be.meeting.repository;

import com.haeyaji.be.meeting.domain.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 참여자 조회는 기본적으로 {@link InviteStatus#ACCEPTED}만 대상으로 한다 —
 * 초대만 받고 수락하지 않은 PENDING이 참여 인원·히트맵 분모·응답 권한에 섞이면 집계가 틀어지기 때문.
 * PENDING은 "받은 초대함"({@link #findByMemberIdAndInviteStatus}) 조회에만 쓴다.
 */
public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipantEntity, UUID> {

    /** 상태 무관 조회 — 초대·수락 전이(재초대 포함) 처리용. */
    Optional<MeetingParticipantEntity> findByMeetingIdAndMemberId(UUID meetingId, UUID memberId);

    boolean existsByMeetingIdAndMemberIdAndInviteStatus(UUID meetingId, UUID memberId, InviteStatus inviteStatus);

    List<MeetingParticipantEntity> findByMeetingIdAndInviteStatusOrderByJoinedAt(UUID meetingId, InviteStatus inviteStatus);

    List<MeetingParticipantEntity> findByMeetingIdAndMemberIdIn(UUID meetingId, Collection<UUID> memberIds);

    long countByMeetingIdAndInviteStatus(UUID meetingId, InviteStatus inviteStatus);

    /** 내가 받은 초대함(PENDING) — 알림이 유실돼도 여기서 다시 찾을 수 있다. */
    List<MeetingParticipantEntity> findByMemberIdAndInviteStatus(UUID memberId, InviteStatus inviteStatus);

    /** 목록 조회용 약속별 참여 인원 수(수락자만). */
    @Query("""
            select new com.haeyaji.be.meeting.repository.MeetingParticipantCount(p.meetingId, count(p))
            from MeetingParticipantEntity p
            where p.meetingId in :meetingIds and p.inviteStatus = :status
            group by p.meetingId""")
    List<MeetingParticipantCount> countByMeetingIds(@Param("meetingIds") Collection<UUID> meetingIds,
                                                    @Param("status") InviteStatus status);
}
