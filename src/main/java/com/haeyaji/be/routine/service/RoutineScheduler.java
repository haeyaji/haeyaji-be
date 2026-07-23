package com.haeyaji.be.routine.service;

import com.haeyaji.be.routine.dto.RoutineApplyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 매일 00시에 전 회원의 활성 루틴을 그날 하루치 todo로 자동 등록한다.
 * 수동 호출용 POST /api/routines/apply(ROUT-4)와 핵심 로직({@link RoutineService#applyRoutine})을 재사용하되,
 * 대상 범위는 {@link RoutineService#applyRoutinesForAllMembers}로 전 회원을 대상으로 한다 (컨트롤러는 본인 루틴만).
 * 중복 방지(ROUT-5)도 그대로 적용되어, 수동 호출과 겹쳐도 안전하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoutineScheduler {

    private static final ZoneId SCHEDULE_ZONE = ZoneId.of("Asia/Seoul");

    private final RoutineService routineService;
    private final Clock clock;

    /**
     * cron은 명시적으로 Asia/Seoul 자정에 맞춰져 있는데, 주입되는 {@link Clock}은 JVM 기본 타임존
     * 기준(운영 컨테이너가 UTC일 수 있음)이라 그대로 {@code LocalDate.now(clock)}을 쓰면 자정 근처에
     * 날짜가 하루 밀릴 수 있었다(N1). 그래서 clock의 시각은 그대로 쓰되 zone만 Seoul로 강제한다.
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void applyTodayRoutines() {
        LocalDate today = LocalDate.now(clock.withZone(SCHEDULE_ZONE));
        RoutineApplyResponse result = routineService.applyRoutinesForAllMembers(today, today);
        log.info("루틴 자동 일괄등록: date={} created={}", today, result.created());
    }
}
