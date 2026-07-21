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

    public List<Todo> getTodosByDate(LocalDate date) {
        return todoRepository.findByTodoDate(date).stream()
                .map(TodoEntity::toDomain)
                .toList();
    }

    @Transactional
    public Todo createTodo(TodoRequest request) {
        if (request.date().isBefore(LocalDate.now(clock))) {
            throw new BusinessException(ErrorCode.PAST_DATE_NOT_ALLOWED);
        }
        TodoSource source = request.source() != null ? request.source() : TodoSource.MANUAL;
        boolean pinned = request.pinned() != null ? request.pinned() : false;
        int sortOrder = request.sortOrder() != null ? request.sortOrder() : 0;
        TodoEntity entity = TodoEntity.create(
                request.title(),
                request.date(),
                request.time(),
                request.placeName(),
                request.placeUrl(),
                request.lat(),
                request.lng(),
                request.category(),
                source,
                pinned,
                sortOrder
        );
        return todoRepository.save(entity).toDomain();
    }

    @Transactional
    public Todo updateTodo(UUID id, TodoUpdateRequest request) {
        TodoEntity entity = todoRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        boolean pinned = request.pinned() != null ? request.pinned() : false;
        int sortOrder = request.sortOrder() != null ? request.sortOrder() : 0;
        entity.update(request.title(), request.time(),
                request.placeName(), request.placeUrl(), request.lat(), request.lng(), request.category(),
                pinned, sortOrder);
        if (request.completed() != null) {
            entity.setCompleted(request.completed(), LocalDateTime.now(clock));
        }
        return entity.toDomain();
    }

    @Transactional
    public void deleteTodo(UUID id) {
        TodoEntity entity = todoRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        todoRepository.delete(entity);
    }
}
