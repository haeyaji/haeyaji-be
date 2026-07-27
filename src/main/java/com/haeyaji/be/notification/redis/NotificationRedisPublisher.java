package com.haeyaji.be.notification.redis;

import com.haeyaji.be.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationRedisPublisher {

    public static final String CHANNEL_PREFIX = "notification:";

    private final RedisTemplate<String, NotificationResponse> notificationRedisTemplate;

    public void publish(UUID memberId, NotificationResponse response) {
        notificationRedisTemplate.convertAndSend(CHANNEL_PREFIX + memberId, response);
    }
}
