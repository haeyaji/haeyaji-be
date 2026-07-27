package com.haeyaji.be.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 앱 전체에서 공유하는 Redis pub/sub 구독 컨테이너.
 * 기능마다 이 빈을 따로 선언하면 그만큼 전용 구독 커넥션이 늘어나므로, 하나만 만들어 공유한다.
 * 실제 채널 구독 등록/해제는 각 기능(예: notification/sse/SseConnectionRegistry)이 이 빈을 주입받아 처리한다.
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }
}
