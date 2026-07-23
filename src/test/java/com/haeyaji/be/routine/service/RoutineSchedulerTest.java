package com.haeyaji.be.routine.service;

import com.haeyaji.be.routine.dto.RoutineApplyResponse;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoutineSchedulerTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 22);
    private final Clock fixedClock = Clock.fixed(
            ZonedDateTime.of(2026, 7, 22, 0, 0, 0, 0, ZONE).toInstant(), ZONE);

    @Test
    void 자정에_오늘_하루치만_일괄등록을_호출한다() {
        RoutineService routineService = mock(RoutineService.class);
        when(routineService.applyRoutines(TODAY, TODAY)).thenReturn(new RoutineApplyResponse(2));
        RoutineScheduler scheduler = new RoutineScheduler(routineService, fixedClock);

        scheduler.applyTodayRoutines();

        verify(routineService).applyRoutines(TODAY, TODAY);
    }
}
