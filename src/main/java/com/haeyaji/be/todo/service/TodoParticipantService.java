package com.haeyaji.be.todo.service;

import com.haeyaji.be.common.exception.BusinessException;
import com.haeyaji.be.common.exception.ErrorCode;
import com.haeyaji.be.todo.domain.InviteStatus;
import com.haeyaji.be.todo.domain.ParticipantRole;
import com.haeyaji.be.todo.domain.Todo;
import com.haeyaji.be.todo.domain.TodoParticipant;
import com.haeyaji.be.todo.dto.TodoShareRequest;
import com.haeyaji.be.todo.repository.TodoEntity;
import com.haeyaji.be.todo.repository.TodoParticipantEntity;
import com.haeyaji.be.todo.repository.TodoParticipantRepository;
import com.haeyaji.be.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 할 일 공유(SHARE-1~8) 담당. 소유자는 todo.member_id로만 판단하고
 * todo_participant엔 OWNER 행을 두지 않는다 — 공유받은 사람만 여기서 관리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoParticipantService {

    private final TodoParticipantRepository participantRepository;
    private final TodoRepository todoRepository;

    @Transactional
    public List<TodoParticipant> share(UUID ownerId, UUID todoId, TodoShareRequest request) {
        requireOwnedTodo(ownerId, todoId);
        List<TodoParticipantEntity> invited = request.members().stream()
                .map(member -> inviteOne(todoId, ownerId, member))
                .toList();
        return participantRepository.saveAll(invited).stream()
                .map(TodoParticipantEntity::toDomain)
                .toList();
    }

    private TodoParticipantEntity inviteOne(UUID todoId, UUID ownerId, TodoShareRequest.ShareMember member) {
        if (member.role() == ParticipantRole.OWNER) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
        if (member.memberId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
        if (participantRepository.existsByTodoIdAndMemberId(todoId, member.memberId())) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
        return TodoParticipantEntity.invite(todoId, member.memberId(), member.role());
    }

    @Transactional
    public TodoParticipant respond(UUID memberId, UUID todoId, boolean accept) {
        TodoParticipantEntity participant = participantRepository.findByTodoIdAndMemberId(todoId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (participant.getInviteStatus() != InviteStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
        if (accept) {
            participant.accept();
        } else {
            participant.reject();
        }
        return participant.toDomain();
    }

    public List<TodoParticipant> getParticipants(UUID requesterId, UUID todoId) {
        TodoEntity todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!todo.getMemberId().equals(requesterId)) {
            requireAcceptedParticipant(todoId, requesterId);
        }
        return participantRepository.findByTodoId(todoId).stream()
                .map(TodoParticipantEntity::toDomain)
                .toList();
    }

    @Transactional
    public TodoParticipant changeRole(UUID ownerId, UUID todoId, UUID targetMemberId, ParticipantRole role) {
        if (role == ParticipantRole.OWNER) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
        requireOwnedTodo(ownerId, todoId);
        TodoParticipantEntity participant = participantRepository.findByTodoIdAndMemberId(todoId, targetMemberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        participant.changeRole(role);
        return participant.toDomain();
    }

    @Transactional
    public void removeParticipant(UUID ownerId, UUID todoId, UUID targetMemberId) {
        requireOwnedTodo(ownerId, todoId);
        TodoParticipantEntity participant = participantRepository.findByTodoIdAndMemberId(todoId, targetMemberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        participantRepository.delete(participant);
    }

    @Transactional
    public void leave(UUID memberId, UUID todoId) {
        TodoParticipantEntity participant = participantRepository.findByTodoIdAndMemberId(todoId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        participantRepository.delete(participant);
    }

    /** SHARE-8. ACCEPTED 상태인 것만 포함 — PENDING 초대는 별도 알림 흐름 몫. */
    public List<Todo> getSharedTodos(UUID memberId) {
        List<UUID> todoIds = participantRepository.findByMemberIdAndInviteStatus(memberId, InviteStatus.ACCEPTED).stream()
                .map(TodoParticipantEntity::getTodoId)
                .toList();
        return todoRepository.findAllById(todoIds).stream()
                .map(TodoEntity::toDomain)
                .toList();
    }

    private TodoEntity requireOwnedTodo(UUID ownerId, UUID todoId) {
        return todoRepository.findByIdAndMemberId(todoId, ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private void requireAcceptedParticipant(UUID todoId, UUID memberId) {
        participantRepository.findByTodoIdAndMemberId(todoId, memberId)
                .filter(p -> p.getInviteStatus() == InviteStatus.ACCEPTED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
