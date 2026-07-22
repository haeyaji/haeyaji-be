package com.haeyaji.be.routine.repository;

import com.haeyaji.be.common.jpa.MutableBaseEntity;
import com.haeyaji.be.routine.domain.Routine;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 루틴 테이블(routine) 매핑. 비즈니스 로직은 여기 두지 않고 {@code domain.Routine}로 변환해 넘긴다.
 * id/createdAt/updatedAt은 {@link MutableBaseEntity}에서 상속.
 */
@Entity
@Table(name = "routine")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoutineEntity extends MutableBaseEntity {

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "routine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoutineDayEntity> routineDays = new ArrayList<>();

    public static RoutineEntity create(String title, LocalTime startTime, Set<DayOfWeek> days) {
        RoutineEntity entity = new RoutineEntity();
        entity.title = title;
        entity.startTime = startTime;
        entity.active = true;
        for (DayOfWeek day : days) {
            entity.routineDays.add(new RoutineDayEntity(entity, day));
        }
        return entity;
    }

    /**
     * 부분 수정. 각 파라미터가 null이면 해당 필드는 기존 값을 그대로 둔다 — 안 보낸 필드가
     * 통째로 지워지는 걸 막기 위함(TodoEntity.update와 동일 패턴). days는 null이 아니면 통째로 교체.
     */
    public void update(String title, LocalTime startTime, Set<DayOfWeek> days, Boolean active) {
        if (title != null) this.title = title;
        if (startTime != null) this.startTime = startTime;
        if (days != null) {
            this.routineDays.clear();
            for (DayOfWeek day : days) {
                this.routineDays.add(new RoutineDayEntity(this, day));
            }
        }
        if (active != null) this.active = active;
    }

    public Routine toDomain() {
        return Routine.builder()
                .id(getId())
                .title(title)
                .startTime(startTime)
                .active(active)
                .days(routineDays.stream().map(RoutineDayEntity::getDayOfWeek).collect(Collectors.toSet()))
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}
