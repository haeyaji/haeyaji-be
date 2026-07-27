package com.haeyaji.be.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 배치(@Scheduled)와 비동기(@Async) 실행 활성화.
 * <p>{@code @Async}는 알림 메일처럼 "본 요청의 응답을 지연시키면 안 되는 부가 작업"에 쓴다.
 */
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig {
}
