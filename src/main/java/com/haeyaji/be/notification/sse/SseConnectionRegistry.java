package com.haeyaji.be.notification.sse;

import com.haeyaji.be.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.haeyaji.be.notification.redis.NotificationRedisPublisher.CHANNEL_PREFIX;

/**
 * 이 인스턴스가 들고 있는 memberId별 SSE 연결을 관리하면서, 동시에 그 memberId 채널의 Redis 구독 리스너 역할도 겸한다.
 * (원래 별도 NotificationRedisListener 클래스로 뒀었는데, 그러면 이 클래스가 리스너를 의존하고
 * 리스너는 다시 이 클래스를 의존하는 순환 참조가 생겨서 하나로 합침 — "누가 연결돼 있나"와
 * "메시지 왔을 때 뭘 할까"가 사실상 같은 책임이라 합치는 게 자연스럽기도 하다.)
 *
 * 채널이 memberId별로 동적이라, 연결될 때 그 memberId 채널을 구독 등록하고
 * 연결이 끊길 때 반드시 같은 topic으로 구독 해제한다(topic 안 지정하면 이 리스너가 등록된
 * 다른 memberId 채널까지 전부 풀려버리므로 주의).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseConnectionRegistry implements MessageListener {

    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final RedisTemplate<String, NotificationResponse> notificationRedisTemplate;

    public SseEmitter register(UUID memberId) {

        SseEmitter emitter = new SseEmitter(0L); // 타임아웃 없음 — 연결을 계속 유지
        emitters.put(memberId, emitter);
        redisMessageListenerContainer.addMessageListener(this, new ChannelTopic(CHANNEL_PREFIX + memberId));

        Runnable cleanup = () -> unregister(memberId);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> unregister(memberId));

        return emitter;
    }

    private void unregister(UUID memberId) {
        emitters.remove(memberId);
        redisMessageListenerContainer.removeMessageListener(this, new ChannelTopic(CHANNEL_PREFIX + memberId));
    }

    public void pushIfPresent(UUID memberId, NotificationResponse payload) {

        SseEmitter emitter = emitters.get(memberId);

        if (emitter == null) {
            return; // 이 인스턴스엔 이 유저 연결이 없음 — 무시
        }

        try {
            emitter.send(SseEmitter.event().name("notification").data(payload));
        } catch (IOException e) {
            log.warn("SSE 전송 실패, 연결 제거: memberId={}", memberId, e);
            unregister(memberId);
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            UUID memberId = UUID.fromString(channel.substring(CHANNEL_PREFIX.length()));

            NotificationResponse response =
                    (NotificationResponse) notificationRedisTemplate.getValueSerializer().deserialize(message.getBody());

            if (response == null) {
                return;
            }

            pushIfPresent(memberId, response);
        } catch (Exception e) {
            log.error("Redis 알림 메시지 처리 실패", e);
        }
    }
}
