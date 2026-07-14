package com.haeyaji.be.weather.client;

import com.haeyaji.be.weather.client.airquality.AirKoreaClient;
import com.haeyaji.be.weather.client.livingidx.KmaUvClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * data.go.kr(생활기상지수·에어코리아) 연결 워밍업.
 * 첫 호출의 TLS 핸드셰이크가 느려(콜드스타트) 사용자 요청이 지연되는 걸 막기 위해,
 * 기동 직후 백그라운드로 한 번 호출해 커넥션 풀을 미리 데운다. (fail-soft라 실패해도 무해)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataGoKrWarmup {

    private final KmaUvClient uvIndexProvider;
    private final AirKoreaClient airQualityProvider;

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        Thread.ofVirtual().name("datakr-warmup").start(() -> {
            try {
                uvIndexProvider.getUvIndex(37.5665, 126.9780, LocalDate.now());
                airQualityProvider.getAirQuality(37.5665, 126.9780);
                log.info("data.go.kr connections warmed up");
            } catch (Exception e) {
                log.debug("data.go.kr warmup skipped: {}", e.toString());
            }
        });
    }
}
