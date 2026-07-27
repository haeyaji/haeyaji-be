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

    // sortOrder까지 같으면(예: 둘 다 기본값 0) 순서가 비결정적이라(N6), createdAt을 2차 정렬키로 둔다.
    List<TodoEntity> findByMemberIdAndTodoDateOrderByPinnedDescSortOrderAscCreatedAtAsc(UUID memberId, LocalDate todoDate);

    /** 라벨 필터(내 소유 한정). 정렬 규칙은 날짜별 조회와 동일하게 맞춘다. */
    List<TodoEntity> findByMemberIdAndTodoDateAndLabelIdOrderByPinnedDescSortOrderAscCreatedAtAsc(
            UUID memberId, LocalDate todoDate, UUID labelId);

    /** 공유받은 할 일을 캘린더(날짜별)에 합치기 위한 조회 — 참여자 todoId 집합 중 해당 날짜 것. */
    List<TodoEntity> findByIdInAndTodoDate(java.util.Collection<UUID> ids, LocalDate todoDate);

    Optional<TodoEntity> findByIdAndMemberId(UUID id, UUID memberId);

    boolean existsByTodoDateAndSourceAndSourceRefId(LocalDate todoDate, TodoSource source, UUID sourceRefId);

    /** 원본(약속·루틴)이 사라졌을 때 거기서 파생된 할 일을 회수하기 위한 조회. */
    List<TodoEntity> findBySourceAndSourceRefId(TodoSource source, UUID sourceRefId);

    boolean existsByLabelId(UUID labelId);

    /** 개인화 distill: 최근 AI 추천으로 담은 할 일(제목/장소를 recentSelections 근거로 사용). */
    List<TodoEntity> findTop10ByMemberIdAndSourceOrderByCreatedAtDesc(UUID memberId, TodoSource source);

    /**
     * 시작 시간 알림 대상. 시간을 정해둔 미완료 할 일만 본다 — 시간이 없으면 언제 알릴지 정할 수 없고,
     * 이미 끝낸 일은 알릴 이유가 없다.
     * <p>약속에서 자동 생성된 할 일은 제외한다 — 같은 약속을 MEETING_REMINDER가 이미 알린다.
     */
    List<TodoEntity> findByTodoDateAndStartTimeBetweenAndStatusAndSourceNot(
            LocalDate todoDate, LocalTime from, LocalTime to, TodoStatus status, TodoSource source);

    /** 날씨 알림 대상. 좌표가 있어야 그 자리 날씨를 볼 수 있으므로 장소를 붙인 할 일만 본다. */
    List<TodoEntity> findByTodoDateAndStatusAndLatIsNotNullAndLngIsNotNull(
            LocalDate todoDate, TodoStatus status);
}
