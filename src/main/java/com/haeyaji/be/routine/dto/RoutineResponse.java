package com.haeyaji.be.routine.dto;

import com.haeyaji.be.routine.domain.DayPreset;
import com.haeyaji.be.routine.domain.Routine;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

/**
 * 루틴 응답 (camelCase). preset은 저장된 days를 보고 be가 판정해서 내려준다(ROUT-2).
 */
public record RoutineResponse(
        UUID id,
        String title,
        LocalTime startTime,
        boolean active,
        Set<DayOfWeek> days,
        DayPreset preset
) {

    public static RoutineResponse from(Routine routine) {
        return new RoutineResponse(
                routine.id(),
                routine.title(),
                routine.startTime(),
                routine.active(),
                routine.days(),
                routine.preset()
        );
    }
}
