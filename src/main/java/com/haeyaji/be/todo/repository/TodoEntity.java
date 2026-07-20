package com.haeyaji.be.todo.repository;

import com.haeyaji.be.todo.domain.Todo;
import com.haeyaji.be.todo.domain.TodoSource;
import com.haeyaji.be.todo.domain.TodoStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 할 일 테이블(todo) 매핑. 비즈니스 로직은 여기 두지 않고 {@code domain.Todo}로 변환해 넘긴다.
 */
@Entity
@Table(name = "todo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TodoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "todo_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "todo_date", nullable = false)
    private LocalDate todoDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(length = 200)
    private String location;

    @Column(length = 30)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TodoSource source;

    @Column(name = "source_ref_id")
    private Long sourceRefId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TodoStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static TodoEntity create(String title, LocalDate todoDate, LocalTime startTime,
            String location, String category, TodoSource source) {
        TodoEntity entity = new TodoEntity();
        entity.title = title;
        entity.todoDate = todoDate;
        entity.startTime = startTime;
        entity.location = location;
        entity.category = category;
        entity.source = source;
        entity.status = TodoStatus.TODO;
        return entity;
    }

    public void update(String title, LocalTime startTime, String location, String category) {
        this.title = title;
        this.startTime = startTime;
        this.location = location;
        this.category = category;
    }

    public Todo toDomain() {
        return Todo.builder()
                .id(id)
                .title(title)
                .todoDate(todoDate)
                .startTime(startTime)
                .location(location)
                .category(category)
                .source(source)
                .sourceRefId(sourceRefId)
                .status(status)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
