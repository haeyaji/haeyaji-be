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
        when(routineService.applyRoutinesForAllMembers(TODAY, TODAY)).thenReturn(new RoutineApplyResponse(2));
        RoutineScheduler scheduler = new RoutineScheduler(routineService, fixedClock);

        scheduler.applyTodayRoutines();

        verify(routineService).applyRoutinesForAllMembers(TODAY, TODAY);
    }

    @Test
    void 주입된_clock이_UTC여도_서울_날짜_기준으로_계산한다() {
        // 2026-07-27(월) 00:00 KST = 2026-07-26(일) 15:00 UTC — clock의 zone이 UTC라도
        // 서울 자정에 걸쳐있는 이 시각엔 "오늘"이 월요일이어야 한다(N1 회귀 방지).
        LocalDate expectedSeoulDate = LocalDate.of(2026, 7, 27);
        Clock utcClock = Clock.fixed(
                ZonedDateTime.of(2026, 7, 26, 15, 0, 0, 0, ZoneId.of("UTC")).toInstant(), ZoneId.of("UTC"));
        RoutineService routineService = mock(RoutineService.class);
        when(routineService.applyRoutinesForAllMembers(expectedSeoulDate, expectedSeoulDate))
                .thenReturn(new RoutineApplyResponse(1));
        RoutineScheduler scheduler = new RoutineScheduler(routineService, utcClock);

        scheduler.applyTodayRoutines();

        verify(routineService).applyRoutinesForAllMembers(expectedSeoulDate, expectedSeoulDate);
    }
}
