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
}
