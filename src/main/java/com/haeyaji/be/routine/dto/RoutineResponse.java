package com.haeyaji.be.routine.dto;

import com.haeyaji.be.routine.domain.Routine;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

/**
 * 루틴 응답 (camelCase).
 */
public record RoutineResponse(
        UUID id,
        String title,
        LocalTime startTime,
        boolean active,
        Set<DayOfWeek> days
) {

    public static RoutineResponse from(Routine routine) {
        return new RoutineResponse(
                routine.id(),
                routine.title(),
                routine.startTime(),
                routine.active(),
                routine.days()
        );
    }
}
