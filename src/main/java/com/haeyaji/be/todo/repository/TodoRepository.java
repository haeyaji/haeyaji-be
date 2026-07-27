package com.haeyaji.be.todo.repository;

import com.haeyaji.be.todo.domain.TodoSource;
import com.haeyaji.be.todo.domain.TodoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TodoRepository extends JpaRepository<TodoEntity, UUID> {

    /** TODO_REMINDER 스케줄러용 — 해당 날짜·시작 시각이 정확히 일치하고 완료되지 않은 할 일 조회. */
    List<TodoEntity> findByTodoDateAndStartTimeAndStatusNot(LocalDate todoDate, LocalTime startTime, TodoStatus status);

    // sortOrder까지 같으면(예: 둘 다 기본값 0) 순서가 비결정적이라(N6), createdAt을 2차 정렬키로 둔다.
    List<TodoEntity> findByMemberIdAndTodoDateOrderByPinnedDescSortOrderAscCreatedAtAsc(UUID memberId, LocalDate todoDate);

    /** 공유받은 할 일을 캘린더(날짜별)에 합치기 위한 조회 — 참여자 todoId 집합 중 해당 날짜 것. */
    List<TodoEntity> findByIdInAndTodoDate(java.util.Collection<UUID> ids, LocalDate todoDate);

    Optional<TodoEntity> findByIdAndMemberId(UUID id, UUID memberId);

    boolean existsByTodoDateAndSourceAndSourceRefId(LocalDate todoDate, TodoSource source, UUID sourceRefId);

    boolean existsByLabelId(UUID labelId);

    /** 개인화 distill: 최근 AI 추천으로 담은 할 일(제목/장소를 recentSelections 근거로 사용). */
    List<TodoEntity> findTop10ByMemberIdAndSourceOrderByCreatedAtDesc(UUID memberId, TodoSource source);
}
