package com.haeyaji.be.routine.service;

import com.haeyaji.be.routine.dto.RoutineApplyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * 매일 00시에 활성 루틴을 그날 하루치 todo로 자동 등록한다.
 * 수동 호출용 POST /api/routines/apply(ROUT-4)와 같은 로직({@link RoutineService#applyRoutines})을 재사용 —
 * 중복 방지(ROUT-5)도 그대로 적용되어, 수동 호출과 겹쳐도 안전하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoutineScheduler {

    private final RoutineService routineService;
    private final Clock clock;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void applyTodayRoutines() {
        LocalDate today = LocalDate.now(clock);
        RoutineApplyResponse result = routineService.applyRoutines(today, today);
        log.info("루틴 자동 일괄등록: date={} created={}", today, result.created());
    }
}
