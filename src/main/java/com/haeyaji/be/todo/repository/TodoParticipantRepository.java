package com.haeyaji.be.todo.repository;

import com.haeyaji.be.todo.domain.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TodoParticipantRepository extends JpaRepository<TodoParticipantEntity, UUID> {

    Optional<TodoParticipantEntity> findByTodoIdAndMemberId(UUID todoId, UUID memberId);

    boolean existsByTodoIdAndMemberId(UUID todoId, UUID memberId);

    List<TodoParticipantEntity> findByTodoId(UUID todoId);

    List<TodoParticipantEntity> findByMemberIdAndInviteStatus(UUID memberId, InviteStatus inviteStatus);

    /** 여러 할 일의 참여자를 한 번에 — 리마인더가 할 일마다 조회하면 쿼리가 건수만큼 늘어난다. */
    List<TodoParticipantEntity> findByTodoIdInAndInviteStatus(java.util.Collection<UUID> todoIds, InviteStatus inviteStatus);

    /** 소유자가 할 일을 지울 때 참여자 행이 고아로 남지 않게 함께 정리(실제 FK 제약이 없다). */
    void deleteByTodoId(UUID todoId);
}
