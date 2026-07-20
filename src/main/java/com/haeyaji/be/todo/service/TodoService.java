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

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;

    public List<Todo> getTodosByDate(LocalDate date) {
        return todoRepository.findByTodoDate(date).stream()
                .map(TodoEntity::toDomain)
                .toList();
    }

    @Transactional
    public Todo createTodo(TodoRequest request) {
        TodoSource source = request.source() != null ? request.source() : TodoSource.MANUAL;
        TodoEntity entity = TodoEntity.create(
                request.title(),
                request.todoDate(),
                request.startTime(),
                request.location(),
                request.category(),
                source
        );
        return todoRepository.save(entity).toDomain();
    }

    @Transactional
    public Todo updateTodo(Long id, TodoUpdateRequest request) {
        TodoEntity entity = todoRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        entity.update(request.title(), request.startTime(), request.location(), request.category());
        return entity.toDomain();
    }

    @Transactional
    public Todo toggleTodo(Long id) {
        TodoEntity entity = todoRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        entity.toggleComplete();
        return entity.toDomain();
    }

    @Transactional
    public void deleteTodo(Long id) {
        TodoEntity entity = todoRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        todoRepository.delete(entity);
    }
}
