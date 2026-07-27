package com.haeyaji.be.notification.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class TodoReminderScheduler {

    private final Clock clock;

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void remindUpcomingTodos() {
        LocalDateTime now = LocalDateTime.now(clock.withZone(ZoneId.of("Asia/Seoul")));
        // 조회 → 알림 발송
    }
}
