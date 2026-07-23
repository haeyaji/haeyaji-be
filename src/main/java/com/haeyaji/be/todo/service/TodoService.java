package com.haeyaji.be.todo.service;

import com.haeyaji.be.common.exception.BusinessException;
import com.haeyaji.be.common.exception.ErrorCode;
import com.haeyaji.be.todo.domain.Todo;
import com.haeyaji.be.todo.domain.TodoSource;
import com.haeyaji.be.todo.dto.TodoRequest;
import com.haeyaji.be.todo.dto.TodoUpdateRequest;
import com.haeyaji.be.todo.repository.TodoEntity;
import com.haeyaji.be.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;
    private final Clock clock;

    public List<Todo> getTodosByDate(UUID memberId, LocalDate date) {
        return todoRepository.findByMemberIdAndTodoDateOrderByPinnedDescSortOrderAscCreatedAtAsc(memberId, date).stream()
                .map(TodoEntity::toDomain)
                .toList();
    }

    @Transactional
    public Todo createTodo(UUID memberId, TodoRequest request) {
        if (request.date().isBefore(LocalDate.now(clock))) {
            throw new BusinessException(ErrorCode.PAST_DATE_NOT_ALLOWED);
        }
        TodoSource source = request.source() != null ? request.source() : TodoSource.MANUAL;
        boolean pinned = request.pinned() != null ? request.pinned() : false;
        int sortOrder = request.sortOrder() != null ? request.sortOrder() : 0;
        TodoEntity entity = TodoEntity.create(
                memberId,
                request.title(),
                request.date(),
                request.time(),
                request.placeName(),
                request.placeUrl(),
                request.lat(),
                request.lng(),
                request.labelId(),
                source,
                pinned,
                sortOrder
        );
        return todoRepository.save(entity).toDomain();
    }

    @Transactional
    public Todo updateTodo(UUID memberId, UUID id, TodoUpdateRequest request) {
        if (request.title() != null && request.title().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
        TodoEntity entity = todoRepository.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        entity.update(request.title(), request.time(),
                request.placeName(), request.placeUrl(), request.lat(), request.lng(),
                request.labelId(), request.pinned(), request.sortOrder());
        if (request.completed() != null) {
            entity.setCompleted(request.completed(), LocalDateTime.now(clock));
        }
        return entity.toDomain();
    }

    @Transactional
    public void deleteTodo(UUID memberId, UUID id) {
        TodoEntity entity = todoRepository.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        todoRepository.delete(entity);
    }
}
