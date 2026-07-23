package com.haeyaji.be.todo.repository;

import com.haeyaji.be.common.jpa.MutableBaseEntity;
import com.haeyaji.be.todo.domain.Todo;
import com.haeyaji.be.todo.domain.TodoSource;
import com.haeyaji.be.todo.domain.TodoStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * 할 일 테이블(todo) 매핑. 비즈니스 로직은 여기 두지 않고 {@code domain.Todo}로 변환해 넘긴다.
 * id/createdAt/updatedAt은 {@link MutableBaseEntity}에서 상속.
 * member_id는 ERD상 NN이지만, user 도메인(인증) 미구현으로 당분간 nullable로 둔다 —
 * RoutineEntity.memberId·LabelEntity.memberId와 같은 느슨한 UUID 컬럼 방식(로그인 붙을 때 값 채움).
 * label_id도 label 엔티티를 참조하는 느슨한 UUID 컬럼(실제 FK 연관관계 아님).
 */
@Entity
@Table(name = "todo", uniqueConstraints = @UniqueConstraint(
        name = "uk_todo_routine_dedup", columnNames = {"todo_date", "source", "source_ref_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TodoEntity extends MutableBaseEntity {

    /** 낙관적 락(N9) — 동시에 같은 row를 수정할 때 나중 쓰기가 먼저 쓴 걸 조용히 덮어쓰는 걸 막는다. */
    @Version
    private Long version;

    @Column(name = "member_id")
    private UUID memberId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "todo_date", nullable = false)
    private LocalDate todoDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "place_name", length = 100)
    private String placeName;

    @Column(name = "place_url", length = 300)
    private String placeUrl;

    private Double lat;

    private Double lng;

    @Column(length = 30)
    private String category;

    @Column(name = "label_id")
    private UUID labelId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TodoSource source;

    @Column(name = "source_ref_id")
    private UUID sourceRefId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TodoStatus status;

    @Column(nullable = false)
    private boolean pinned;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public static TodoEntity create(UUID memberId, String title, LocalDate todoDate, LocalTime startTime,
            String placeName, String placeUrl, Double lat, Double lng, String category, UUID labelId,
            TodoSource source, boolean pinned, int sortOrder) {
        TodoEntity entity = new TodoEntity();
        entity.memberId = memberId;
        entity.title = title;
        entity.todoDate = todoDate;
        entity.startTime = startTime;
        entity.placeName = placeName;
        entity.placeUrl = placeUrl;
        entity.lat = lat;
        entity.lng = lng;
        entity.category = category;
        entity.labelId = labelId;
        entity.source = source;
        entity.status = TodoStatus.TODO;
        entity.pinned = pinned;
        entity.sortOrder = sortOrder;
        return entity;
    }
    
    /**
     * 루틴 일괄 등록(ROUT-4)용 생성. source=ROUTINE 고정, sourceRefId로 원본 루틴을 추적한다.
     */
    public static TodoEntity createFromRoutine(UUID memberId, String title, LocalDate todoDate, LocalTime startTime, UUID routineId) {
        TodoEntity entity = create(memberId, title, todoDate, startTime, null, null, null, null, null, null, TodoSource.ROUTINE, false, 0);
        entity.sourceRefId = routineId;
        return entity;
    }


    /**
     * 부분 수정. 각 파라미터가 null이면 해당 필드는 기존 값을 그대로 둔다 — 안 보낸 필드가
     * 통째로 지워지는 걸 막기 위함(TODO-4 부분수정 버그 수정).
     */
    public void update(String title, LocalTime startTime,
            String placeName, String placeUrl, Double lat, Double lng, String category, UUID labelId,
            Boolean pinned, Integer sortOrder) {
        if (title != null) this.title = title;
        if (startTime != null) this.startTime = startTime;
        if (placeName != null) this.placeName = placeName;
        if (placeUrl != null) this.placeUrl = placeUrl;
        if (lat != null) this.lat = lat;
        if (lng != null) this.lng = lng;
        if (category != null) this.category = category;
        if (labelId != null) this.labelId = labelId;
        if (pinned != null) this.pinned = pinned;
        if (sortOrder != null) this.sortOrder = sortOrder;
    }

    public void setCompleted(boolean completed, LocalDateTime now) {
        if (completed) {
            this.status = TodoStatus.DONE;
            this.endedAt = now;
        } else {
            this.status = TodoStatus.TODO;
            this.endedAt = null;
        }
    }

    public Todo toDomain() {
        return Todo.builder()
                .id(getId())
                .memberId(memberId)
                .title(title)
                .todoDate(todoDate)
                .startTime(startTime)
                .endedAt(endedAt)
                .placeName(placeName)
                .placeUrl(placeUrl)
                .lat(lat)
                .lng(lng)
                .category(category)
                .labelId(labelId)
                .source(source)
                .sourceRefId(sourceRefId)
                .status(status)
                .pinned(pinned)
                .sortOrder(sortOrder)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}
