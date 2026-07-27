package com.haeyaji.be.notification.sse;

import com.haeyaji.be.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
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
 * <p>채널이 memberId별로 동적이라, 연결될 때 그 memberId 채널을 구독 등록하고
 * 연결이 끊길 때 반드시 같은 topic으로 구독 해제한다(topic 안 지정하면 이 리스너가 등록된
 * 다른 memberId 채널까지 전부 풀려버리므로 주의).
 *
 * <p><b>재연결 경합 주의</b>: 새로고침·두 번째 탭이면 같은 memberId로 새 연결이 들어오는데,
 * 뒤늦게 도착한 <i>옛</i> 연결의 종료 콜백이 새 연결의 엔트리를 지우면 새 연결은 아무것도 못 받는
 * 좀비가 된다. 그래서 해제는 항상 "내가 넣은 그 emitter일 때만"(compare-and-remove) 수행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseConnectionRegistry implements MessageListener {

    /** 무한 대기는 죽은 연결을 영원히 남긴다. 만료되면 브라우저가 자동 재연결한다. */
    private static final long EMITTER_TIMEOUT_MS = Duration.ofMinutes(30).toMillis();
    /** 프록시·LB가 유휴 연결을 끊지 않도록 주기적으로 주석 이벤트를 흘려보낸다. */
    private static final long HEARTBEAT_MS = 25_000L;

    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final RedisTemplate<String, NotificationResponse> notificationRedisTemplate;

    public SseEmitter register(UUID memberId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        try {
            // 구독을 먼저 건다 — Redis가 죽어 실패하면 맵에 유령 엔트리를 남기지 않는다.
            redisMessageListenerContainer.addMessageListener(this, topicOf(memberId));
        } catch (RuntimeException e) {
            emitter.completeWithError(e);
            throw e;
        }
        // 같은 회원의 기존 연결(새로고침·다른 탭)은 정리하고 새 연결로 교체한다.
        SseEmitter previous = emitters.put(memberId, emitter);
        if (previous != null) {
            previous.complete();
        }

        emitter.onCompletion(() -> unregister(memberId, emitter));
        emitter.onTimeout(() -> unregister(memberId, emitter));
        emitter.onError(e -> unregister(memberId, emitter));
        return emitter;
    }

    /** 내가 등록한 emitter일 때만 해제한다 — 옛 연결의 뒤늦은 콜백이 새 연결을 끊지 않도록. */
    private void unregister(UUID memberId, SseEmitter emitter) {
        if (emitters.remove(memberId, emitter)) {
            redisMessageListenerContainer.removeMessageListener(this, topicOf(memberId));
        }
    }

    public void pushIfPresent(UUID memberId, NotificationResponse payload) {
        SseEmitter emitter = emitters.get(memberId);
        if (emitter == null) {
            return; // 이 인스턴스엔 이 유저 연결이 없음 — 무시
        }
        try {
            emitter.send(SseEmitter.event().name("notification").data(payload));
        } catch (Exception e) {
            // IOException(연결 끊김)뿐 아니라 이미 완료된 emitter의 IllegalStateException도 잡아야
            // 죽은 연결이 레지스트리에 영원히 남지 않는다.
            log.warn("SSE 전송 실패, 연결 제거: memberId={} err={}", memberId, e.toString());
            emitter.completeWithError(e);
            unregister(memberId, emitter);
        }
    }

    /** 유휴 연결 유지용 하트비트. 실패한 연결은 여기서 자연히 정리된다. */
    @Scheduled(fixedRate = HEARTBEAT_MS)
    public void heartbeat() {
        emitters.forEach((memberId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().comment("keep-alive"));
            } catch (Exception e) {
                log.debug("SSE 하트비트 실패, 연결 제거: memberId={}", memberId);
                emitter.completeWithError(e);
                unregister(memberId, emitter);
            }
        });
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

    private static ChannelTopic topicOf(UUID memberId) {
        return new ChannelTopic(CHANNEL_PREFIX + memberId);
    }
}
