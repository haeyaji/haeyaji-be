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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * 할 일 테이블(todo) 매핑. 비즈니스 로직은 여기 두지 않고 {@code domain.Todo}로 변환해 넘긴다.
 * id/createdAt/updatedAt은 {@link MutableBaseEntity}에서 상속.
 */
@Entity
@Table(name = "todo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TodoEntity extends MutableBaseEntity {

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "todo_date", nullable = false)
    private LocalDate todoDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "place_name", length = 100)
    private String placeName;

    @Column(name = "place_url", length = 300)
    private String placeUrl;

    private Double lat;

    private Double lng;

    @Column(length = 30)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TodoSource source;

    @Column(name = "source_ref_id")
    private UUID sourceRefId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TodoStatus status;

    public static TodoEntity create(String title, LocalDate todoDate, LocalTime startTime, LocalTime endTime,
            String placeName, String placeUrl, Double lat, Double lng, String category, TodoSource source) {
        TodoEntity entity = new TodoEntity();
        entity.title = title;
        entity.todoDate = todoDate;
        entity.startTime = startTime;
        entity.endTime = endTime;
        entity.placeName = placeName;
        entity.placeUrl = placeUrl;
        entity.lat = lat;
        entity.lng = lng;
        entity.category = category;
        entity.source = source;
        entity.status = TodoStatus.TODO;
        return entity;
    }

    public void update(String title, LocalTime startTime, LocalTime endTime,
            String placeName, String placeUrl, Double lat, Double lng, String category) {
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.placeName = placeName;
        this.placeUrl = placeUrl;
        this.lat = lat;
        this.lng = lng;
        this.category = category;
    }

    public void toggleComplete() {
        this.status = (this.status == TodoStatus.DONE) ? TodoStatus.TODO : TodoStatus.DONE;
    }

    public Todo toDomain() {
        return Todo.builder()
                .id(getId())
                .title(title)
                .todoDate(todoDate)
                .startTime(startTime)
                .endTime(endTime)
                .placeName(placeName)
                .placeUrl(placeUrl)
                .lat(lat)
                .lng(lng)
                .category(category)
                .source(source)
                .sourceRefId(sourceRefId)
                .status(status)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}
