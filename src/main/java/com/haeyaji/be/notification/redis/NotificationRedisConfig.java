package com.haeyaji.be.notification.redis;

import com.haeyaji.be.notification.dto.NotificationResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 알림 전용 Redis 직렬화 설정. 구독용 RedisMessageListenerContainer는 앱 전체가 공유하는
 * {@link com.haeyaji.be.config.RedisConfig}에 있고, 여기서는 notification 값 타입에 맞는 Template만 둔다.
 */
@Configuration
public class NotificationRedisConfig {

    @Bean
    public RedisTemplate<String, NotificationResponse> notificationRedisTemplate(RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, NotificationResponse> template = new RedisTemplate<>();

        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JacksonJsonRedisSerializer<>(NotificationResponse.class));

        return template;
    }
}
