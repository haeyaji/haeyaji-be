package com.haeyaji.be.profile.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매주 월요일 04시(KST) 전 회원 가중치 x0.9 decay.
 * 새 신호가 없으면 취향이 서서히 0에 수렴 → 오래된 취향이 최근 것을 계속 이기지 않게 한다.
 * 핵심 로직은 {@link WeightService#decayAll()} 재사용(수동 호출 시에도 동일 동작).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeightDecayScheduler {

    private final WeightService weightService;

    @Scheduled(cron = "0 0 4 * * MON", zone = "Asia/Seoul")
    public void decayWeights() {
        weightService.decayAll();
    }
}
