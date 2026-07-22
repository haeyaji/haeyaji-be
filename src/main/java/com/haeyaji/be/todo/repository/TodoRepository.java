package com.haeyaji.be.todo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TodoRepository extends JpaRepository<TodoEntity, UUID> {

    List<TodoEntity> findByTodoDateOrderByPinnedDescSortOrderAsc(LocalDate todoDate);
}
