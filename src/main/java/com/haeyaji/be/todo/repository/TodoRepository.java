package com.haeyaji.be.todo.repository;

import com.haeyaji.be.todo.domain.TodoSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TodoRepository extends JpaRepository<TodoEntity, UUID> {

    // sortOrder까지 같으면(예: 둘 다 기본값 0) 순서가 비결정적이라(N6), createdAt을 2차 정렬키로 둔다.
    List<TodoEntity> findByMemberIdAndTodoDateOrderByPinnedDescSortOrderAscCreatedAtAsc(UUID memberId, LocalDate todoDate);

    Optional<TodoEntity> findByIdAndMemberId(UUID id, UUID memberId);

    boolean existsByTodoDateAndSourceAndSourceRefId(LocalDate todoDate, TodoSource source, UUID sourceRefId);

    boolean existsByLabelId(UUID labelId);
}
