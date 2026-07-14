package com.haeyaji.be.todo.service;

import com.haeyaji.be.todo.domain.Todo;
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
}
