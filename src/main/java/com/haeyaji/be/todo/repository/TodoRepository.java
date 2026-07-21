package com.haeyaji.be.todo.repository;

import com.haeyaji.be.todo.domain.TodoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TodoRepository extends JpaRepository<TodoEntity, UUID> {

    List<TodoEntity> findByTodoDate(LocalDate todoDate);

    long countByTodoDate(LocalDate todoDate);

    long countByTodoDateAndStatus(LocalDate todoDate, TodoStatus status);
}
