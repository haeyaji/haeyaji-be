package com.haeyaji.be.notification.redis;

import com.haeyaji.be.notification.domain.NotificationCategory;
import com.haeyaji.be.notification.domain.NotificationType;
import com.haeyaji.be.notification.dto.NotificationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 Redis(로컬 docker-compose)를 상대로 memberId별 채널 발행/구독이 실제로 왕복되는지 확인한다.
 * SSE/HTTP/로그인 계층은 거치지 않고, NotificationRedisPublisher가 발행한 걸
 * 같은 채널 이름 규칙("notification:{memberId}")으로 구독한 리스너가 그대로 받는지만 검증한다.
 * 실행 전 docker-compose up으로 Redis가 떠 있어야 한다.
 */
@SpringBootTest
class NotificationRedisPubSubIntegrationTest {

    @Autowired
    private NotificationRedisPublisher notificationRedisPublisher;

    @Autowired
    private RedisMessageListenerContainer redisMessageListenerContainer;

    @Autowired
    private RedisTemplate<String, NotificationResponse> notificationRedisTemplate;

    @Test
    void 발행한_알림이_해당_memberId_채널_구독자에게_도착한다() throws InterruptedException {
        UUID memberId = UUID.randomUUID();
        BlockingQueue<NotificationResponse> received = new LinkedBlockingQueue<>();

        MessageListener testListener = (message, pattern) ->
                received.add((NotificationResponse) notificationRedisTemplate.getValueSerializer().deserialize(message.getBody()));
        ChannelTopic topic = new ChannelTopic(NotificationRedisPublisher.CHANNEL_PREFIX + memberId);
        redisMessageListenerContainer.addMessageListener(testListener, topic);

        try {
            NotificationResponse sent = new NotificationResponse(
                    UUID.randomUUID(),
                    NotificationCategory.INVITE,
                    NotificationType.MEETING_INVITE,
                    "제목",
                    "본문",
                    UUID.randomUUID(),
                    "token",
                    false,
                    LocalDateTime.now(),
                    null
            );

            notificationRedisPublisher.publish(memberId, sent);

            NotificationResponse arrived = received.poll(2, TimeUnit.SECONDS);

            assertThat(arrived).isNotNull();
            assertThat(arrived.type()).isEqualTo(NotificationType.MEETING_INVITE);
            assertThat(arrived.title()).isEqualTo("제목");
        } finally {
            redisMessageListenerContainer.removeMessageListener(testListener, topic);
        }
    }
}
