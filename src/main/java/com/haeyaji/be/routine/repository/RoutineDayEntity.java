package com.haeyaji.be.routine.repository;

import com.haeyaji.be.common.jpa.UuidBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;

/**
 * 루틴의 반복 요일 한 칸(요일 정규화). ERD상 created_at/updated_at 없음 — {@link UuidBaseEntity}만 상속.
 */
@Entity
@Table(name = "routine_day")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoutineDayEntity extends UuidBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id", nullable = false)
    private RoutineEntity routine;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    RoutineDayEntity(RoutineEntity routine, DayOfWeek dayOfWeek) {
        this.routine = routine;
        this.dayOfWeek = dayOfWeek;
    }
}
