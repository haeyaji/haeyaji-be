package com.haeyaji.be.todo.repository;

import com.haeyaji.be.todo.domain.TodoSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TodoRepository extends JpaRepository<TodoEntity, UUID> {

    List<TodoEntity> findByTodoDateOrderByPinnedDescSortOrderAsc(LocalDate todoDate);

    boolean existsByTodoDateAndSourceAndSourceRefId(LocalDate todoDate, TodoSource source, UUID sourceRefId);
}
