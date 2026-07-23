package com.haeyaji.be.todo.service;

import com.haeyaji.be.common.exception.BusinessException;
import com.haeyaji.be.common.exception.ErrorCode;
import com.haeyaji.be.todo.domain.InviteStatus;
import com.haeyaji.be.todo.domain.ParticipantRole;
import com.haeyaji.be.todo.domain.Todo;
import com.haeyaji.be.todo.domain.TodoParticipant;
import com.haeyaji.be.todo.domain.TodoSource;
import com.haeyaji.be.todo.dto.TodoShareRequest;
import com.haeyaji.be.todo.repository.TodoEntity;
import com.haeyaji.be.todo.repository.TodoParticipantEntity;
import com.haeyaji.be.todo.repository.TodoParticipantRepository;
import com.haeyaji.be.todo.repository.TodoRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TodoParticipantServiceTest {

    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID TODO_ID = UUID.randomUUID();

    private TodoEntity ownedTodo() {
        return TodoEntity.create(OWNER_ID, "제목", LocalDate.of(2026, 7, 22), null,
                null, null, null, null, null, TodoSource.MANUAL, false, 0);
    }

    @Test
    void 공유시_owner가_아니면_예외() {
        TodoParticipantRepository participantRepo = mock(TodoParticipantRepository.class);
        TodoRepository todoRepo = mock(TodoRepository.class);
        when(todoRepo.findByIdAndMemberId(TODO_ID, OWNER_ID)).thenReturn(Optional.empty());
        TodoParticipantService service = new TodoParticipantService(participantRepo, todoRepo);
        UUID targetMemberId = UUID.randomUUID();
        var request = new TodoShareRequest(List.of(new TodoShareRequest.ShareMember(targetMemberId, ParticipantRole.EDITOR)));

        assertThatThrownBy(() -> service.share(OWNER_ID, TODO_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void 공유시_role을_OWNER로_지정하면_예외() {
        TodoParticipantRepository participantRepo = mock(TodoParticipantRepository.class);
        TodoRepository todoRepo = mock(TodoRepository.class);
        when(todoRepo.findByIdAndMemberId(TODO_ID, OWNER_ID)).thenReturn(Optional.of(ownedTodo()));
        TodoParticipantService service = new TodoParticipantService(participantRepo, todoRepo);
        UUID targetMemberId = UUID.randomUUID();
        var request = new TodoShareRequest(List.of(new TodoShareRequest.ShareMember(targetMemberId, ParticipantRole.OWNER)));

        assertThatThrownBy(() -> service.share(OWNER_ID, TODO_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    @Test
    void 공유시_자기자신을_초대하면_예외() {
        TodoParticipantRepository participantRepo = mock(TodoParticipantRepository.class);
        TodoRepository todoRepo = mock(TodoRepository.class);
        when(todoRepo.findByIdAndMemberId(TODO_ID, OWNER_ID)).thenReturn(Optional.of(ownedTodo()));
        TodoParticipantService service = new TodoParticipantService(participantRepo, todoRepo);
        var request = new TodoShareRequest(List.of(new TodoShareRequest.ShareMember(OWNER_ID, ParticipantRole.EDITOR)));

        assertThatThrownBy(() -> service.share(OWNER_ID, TODO_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    @Test
    void 공유시_이미_초대된_사람이면_예외() {
        TodoParticipantRepository participantRepo = mock(TodoParticipantRepository.class);
        TodoRepository todoRepo = mock(TodoRepository.class);
        when(todoRepo.findByIdAndMemberId(TODO_ID, OWNER_ID)).thenReturn(Optional.of(ownedTodo()));
        UUID targetMemberId = UUID.randomUUID();
        when(participantRepo.existsByTodoIdAndMemberId(TODO_ID, targetMemberId)).thenReturn(true);
        TodoParticipantService service = new TodoParticipantService(participantRepo, todoRepo);
        var request = new TodoShareRequest(List.of(new TodoShareRequest.ShareMember(targetMemberId, ParticipantRole.EDITOR)));

        assertThatThrownBy(() -> service.share(OWNER_ID, TODO_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    @Test
    void 공유하면_PENDING_상태로_생성된다() {
        TodoParticipantRepository participantRepo = mock(TodoParticipantRepository.class);
        TodoRepository todoRepo = mock(TodoRepository.class);
        when(todoRepo.findByIdAndMemberId(TODO_ID, OWNER_ID)).thenReturn(Optional.of(ownedTodo()));
        when(participantRepo.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        TodoParticipantService service = new TodoParticipantService(participantRepo, todoRepo);
        UUID targetMemberId = UUID.randomUUID();
        var request = new TodoShareRequest(List.of(new TodoShareRequest.ShareMember(targetMemberId, ParticipantRole.EDITOR)));

        List<TodoParticipant> result = service.share(OWNER_ID, TODO_ID, request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).memberId()).isEqualTo(targetMemberId);
        assertThat(result.get(0).role()).isEqualTo(ParticipantRole.EDITOR);
        assertThat(result.get(0).inviteStatus()).isEqualTo(InviteStatus.PENDING);
    }

    private TodoParticipantEntity pendingParticipant(UUID memberId, ParticipantRole role) {
        return TodoParticipantEntity.invite(TODO_ID, memberId, role);
    }

    @Test
    void 초대수락하면_ACCEPTED로_바뀐다() {
        TodoParticipantRepository participantRepo = mock(TodoParticipantRepository.class);
        UUID memberId = UUID.randomUUID();
        TodoParticipantEntity participant = pendingParticipant(memberId, ParticipantRole.EDITOR);
        when(participantRepo.findByTodoIdAndMemberId(TODO_ID, memberId)).thenReturn(Optional.of(participant));
        TodoParticipantService service = new TodoParticipantService(participantRepo, mock(TodoRepository.class));

        TodoParticipant result = service.respond(memberId, TODO_ID, true);

        assertThat(result.inviteStatus()).isEqualTo(InviteStatus.ACCEPTED);
    }

    @Test
    void 초대거절하면_REJECTED로_바뀐다() {
        TodoParticipantRepository participantRepo = mock(TodoParticipantRepository.class);
        UUID memberId = UUID.randomUUID();
        TodoParticipantEntity participant = pendingParticipant(memberId, ParticipantRole.VIEWER);
        when(participantRepo.findByTodoIdAndMemberId(TODO_ID, memberId)).thenReturn(Optional.of(participant));
        TodoParticipantService service = new TodoParticipantService(participantRepo, mock(TodoRepository.class));

        TodoParticipant result = service.respond(memberId, TODO_ID, false);

        assertThat(result.inviteStatus()).isEqualTo(InviteStatus.REJECTED);
    }

    @Test
    void 이미_응답한_초대에_다시_응답하면_예외() {
        TodoParticipantRepository participantRepo = mock(TodoParticipantRepository.class);
        UUID memberId = UUID.randomUUID();
        TodoParticipantEntity participant = pendingParticipant(memberId, ParticipantRole.EDITOR);
        participant.accept();
        when(participantRepo.findByTodoIdAndMemberId(TODO_ID, memberId)).thenReturn(Optional.of(participant));
        TodoParticipantService service = new TodoParticipantService(participantRepo, mock(TodoRepository.class));

        assertThatThrownBy(() -> service.respond(memberId, TODO_ID, true))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    @Test
    void 참여자목록조회는_owner면_가능() {
        TodoParticipantRepository participantRepo = mock(TodoParticipantRepository.class);
        TodoRepository todoRepo = mock(TodoRepository.class);
        when(todoRepo.findById(TODO_ID)).thenReturn(Optional.of(ownedTodo()));
        when(participantRepo.findByTodoId(TODO_ID)).thenReturn(List.of());
        TodoParticipantService service = new TodoParticipantService(participantRepo, todoRepo);

        List<TodoParticipant> result = service.getParticipants(OWNER_ID, TODO_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void 참여자목록조회는_ACCEPTED_참여자면_가능() {
        TodoParticipantRepository participantRepo = mock(TodoParticipantRepository.class);
        TodoRepository todoRepo = mock(TodoRepository.class);
        UUID memberId = UUID.randomUUID();
        TodoParticipantEntity participant = pendingParticipant(memberId, ParticipantRole.VIEWER);
        participant.accept();
        when(todoRepo.findById(TODO_ID)).thenReturn(Optional.of(ownedTodo()));
        when(participantRepo.findByTodoIdAndMemberId(TODO_ID, memberId)).thenReturn(Optional.of(participant));
        when(participantRepo.findByTodoId(TODO_ID)).thenReturn(List.of(participant));
        TodoParticipantService service = new TodoParticipantService(participantRepo, todoRepo);

        List<TodoParticipant> result = service.getParticipants(memberId, TODO_ID);

        assertThat(result).hasSize(1);
    }

    @Test
    void 참여자목록조회는_PENDING_참여자면_불가() {
        TodoParticipantRepository participantRepo = mock(TodoParticipantRepository.class);
        TodoRepository todoRepo = mock(TodoRepository.class);
        UUID memberId = UUID.randomUUID();
        TodoParticipantEntity participant = pendingParticipant(memberId, ParticipantRole.VIEWER);
        when(todoRepo.findById(TODO_ID)).thenReturn(Optional.of(ownedTodo()));
        when(participantRepo.findByTodoIdAndMemberId(TODO_ID, memberId)).thenReturn(Optional.of(participant));
        TodoParticipantService service = new TodoParticipantService(participantRepo, todoRepo);

        assertThatThrownBy(() -> service.getParticipants(memberId, TODO_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void 참여자목록조회는_무관한_사람이면_불가() {
        TodoParticipantRepository participantRepo = mock(TodoParticipantRepository.class);
        TodoRepository todoRepo = mock(TodoRepository.class);
        UUID strangerId = UUID.randomUUID();
        when(todoRepo.findById(TODO_ID)).thenReturn(Optional.of(ownedTodo()));
        when(participantRepo.findByTodoIdAndMemberId(TODO_ID, strangerId)).thenReturn(Optional.empty());
        TodoParticipantService service = new TodoParticipantService(participantRepo, todoRepo);

        assertThatThrownBy(() -> service.getParticipants(strangerId, TODO_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void 역할변경은_owner만_가능() {
        TodoParticipantRepository participantRepo = mock(TodoParticipantRepository.class);
        TodoRepository todoRepo = mock(TodoRepository.class);
        UUID memberId = UUID.randomUUID();
        TodoParticipantEntity participant = pendingParticipant(memberId, ParticipantRole.VIEWER);
        when(todoRepo.findByIdAndMemberId(TODO_ID, OWNER_ID)).thenReturn(Optional.of(ownedTodo()));
        when(participantRepo.findByTodoIdAndMemberId(TODO_ID, memberId)).thenReturn(Optional.of(participant));
        TodoParticipantService service = new TodoParticipantService(participantRepo, todoRepo);

        TodoParticipant result = service.changeRole(OWNER_ID, TODO_ID, memberId, ParticipantRole.EDITOR);

        assertThat(result.role()).isEqualTo(ParticipantRole.EDITOR);
    }

    @Test
    void 역할변경시_OWNER로는_불가() {
        TodoParticipantRepository participantRepo = mock(TodoParticipantRepository.class);
        TodoRepository todoRepo = mock(TodoRepository.class);
        UUID memberId = UUID.randomUUID();

        TodoParticipantService service = new TodoParticipantService(participantRepo, todoRepo);

        assertThatThrownBy(() -> service.changeRole(OWNER_ID, TODO_ID, memberId, ParticipantRole.OWNER))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    @Test
    void 공유해제는_owner만_가능() {
        TodoParticipantRepository participantRepo = mock(TodoParticipantRepository.class);
        TodoRepository todoRepo = mock(TodoRepository.class);
        UUID memberId = UUID.randomUUID();
        TodoParticipantEntity participant = pendingParticipant(memberId, ParticipantRole.VIEWER);
        when(todoRepo.findByIdAndMemberId(TODO_ID, OWNER_ID)).thenReturn(Optional.of(ownedTodo()));
        when(participantRepo.findByTodoIdAndMemberId(TODO_ID, memberId)).thenReturn(Optional.of(participant));
        TodoParticipantService service = new TodoParticipantService(participantRepo, todoRepo);

        service.removeParticipant(OWNER_ID, TODO_ID, memberId);

        verify(participantRepo).delete(participant);
    }

    @Test
    void 나가기는_참여자_본인이_할_수_있다() {
        TodoParticipantRepository participantRepo = mock(TodoParticipantRepository.class);
        UUID memberId = UUID.randomUUID();
        TodoParticipantEntity participant = pendingParticipant(memberId, ParticipantRole.EDITOR);
        participant.accept();
        when(participantRepo.findByTodoIdAndMemberId(TODO_ID, memberId)).thenReturn(Optional.of(participant));
        TodoParticipantService service = new TodoParticipantService(participantRepo, mock(TodoRepository.class));

        service.leave(memberId, TODO_ID);

        verify(participantRepo).delete(participant);
    }

    @Test
    void 나가기는_참여자가_아니면_예외() {
        TodoParticipantRepository participantRepo = mock(TodoParticipantRepository.class);
        UUID memberId = UUID.randomUUID();
        when(participantRepo.findByTodoIdAndMemberId(TODO_ID, memberId)).thenReturn(Optional.empty());
        TodoParticipantService service = new TodoParticipantService(participantRepo, mock(TodoRepository.class));

        assertThatThrownBy(() -> service.leave(memberId, TODO_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
        verify(participantRepo, never()).delete(any(TodoParticipantEntity.class));
    }

    @Test
    void 공유받은_목록조회는_ACCEPTED만_포함한다() {
        TodoParticipantRepository participantRepo = mock(TodoParticipantRepository.class);
        TodoRepository todoRepo = mock(TodoRepository.class);
        UUID memberId = UUID.randomUUID();
        TodoParticipantEntity accepted = pendingParticipant(memberId, ParticipantRole.EDITOR);
        accepted.accept();
        when(participantRepo.findByMemberIdAndInviteStatus(memberId, InviteStatus.ACCEPTED))
                .thenReturn(List.of(accepted));
        when(todoRepo.findAllById(List.of(TODO_ID))).thenReturn(List.of(ownedTodo()));
        TodoParticipantService service = new TodoParticipantService(participantRepo, todoRepo);

        List<Todo> result = service.getSharedTodos(memberId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("제목");
    }
}
