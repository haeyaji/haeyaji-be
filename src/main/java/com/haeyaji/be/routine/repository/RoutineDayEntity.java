package com.haeyaji.be.routine.repository;

import com.haeyaji.be.common.jpa.UuidBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.util.Objects;

/**
 * 루틴의 반복 요일 한 칸(요일 정규화). ERD상 created_at/updated_at 없음 — {@link UuidBaseEntity}만 상속.
 * equals/hashCode를 (routine, dayOfWeek) 업무키 기준으로 재정의 — {@code RoutineEntity.update()}가
 * 요일을 부분 교체할 때(clear+addAll 대신 diff)이 값 비교로 기존/신규를 구분해야 하기 때문.
 * 이게 없으면 매번 전체 삭제 후 재삽입하게 되고, 같은 요일이 남아있는 경우
 * uk_routine_day(routine_id, day_of_week) 유니크 제약과 insert-before-delete 플러시 순서가 충돌해 500이 난다.
 */
@Entity
@Table(name = "routine_day")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoutineDayEntity extends UuidBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id", nullable = false)
    private RoutineEntity routine;

    @Convert(converter = DayOfWeekConverter.class)
    @Column(name = "day_of_week", nullable = false, length = 3)
    private DayOfWeek dayOfWeek;

    RoutineDayEntity(RoutineEntity routine, DayOfWeek dayOfWeek) {
        this.routine = routine;
        this.dayOfWeek = dayOfWeek;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoutineDayEntity that)) return false;
        return dayOfWeek == that.dayOfWeek
                && Objects.equals(routine != null ? routine.getId() : null, that.routine != null ? that.routine.getId() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dayOfWeek);
    }
}
