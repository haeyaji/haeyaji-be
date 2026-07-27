package com.haeyaji.be.notification.sse;

import com.haeyaji.be.notification.dto.NotificationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * SSE 연결 레지스트리 — 새로고침·두 번째 탭으로 생기는 재연결 경합과, 죽은 연결이 남지 않는지.
 *
 * <p>여기서 틀리면 증상이 <b>조용하다</b>: 예외 없이 알림만 안 온다. 그래서 단위로 못 박아 둔다.
 */
class SseConnectionRegistryTest {

    private RedisMessageListenerContainer container;
    private SseConnectionRegistry registry;

    private final UUID memberId = UUID.randomUUID();

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        container = mock(RedisMessageListenerContainer.class);
        registry = new SseConnectionRegistry(container, mock(RedisTemplate.class));
    }

    private static NotificationResponse payload() {
        return mock(NotificationResponse.class);
    }

    @Test
    void 옛_연결의_뒤늦은_종료가_새_연결을_끊지_않는다() {
        // 새로고침·두 번째 탭이면 같은 회원으로 새 연결이 들어온다. 옛 연결의 콜백이 새 엔트리를
        // 지우면 새 연결은 예외 없이 아무것도 못 받는 좀비가 된다.
        SseEmitter first = registry.register(memberId);
        SseEmitter second = registry.register(memberId);
        assertThat(first).isNotSameAs(second);

        registry.unregister(memberId, first); // 옛 연결의 종료 콜백이 뒤늦게 도착

        verify(container, never()).removeMessageListener(any(MessageListener.class), any(Topic.class));
        assertThat(emitterOf(memberId)).isSameAs(second);
    }

    @Test
    void 현재_연결이_끝나면_구독도_함께_해제된다() {
        SseEmitter emitter = registry.register(memberId);

        registry.unregister(memberId, emitter);

        verify(container).removeMessageListener(any(MessageListener.class), any(Topic.class));
        assertThat(emitterOf(memberId)).isNull();
        registry.pushIfPresent(memberId, payload()); // 이미 빠졌으니 조용히 무시돼야 한다
    }

    @Test
    void 구독_등록이_실패하면_연결을_남기지_않는다() {
        // Redis가 죽었는데 맵에만 엔트리가 남으면 아무것도 못 받는 유령 연결이 된다.
        doThrow(new IllegalStateException("redis down"))
                .when(container).addMessageListener(any(MessageListener.class), any(Topic.class));

        assertThatThrownBy(() -> registry.register(memberId)).isInstanceOf(IllegalStateException.class);

        registry.pushIfPresent(memberId, payload()); // 남아 있으면 여기서 터진다
    }

    @Test
    void 전송이_실패한_연결은_레지스트리에서_빠진다() throws IOException {
        // 끊긴 연결을 붙들고 있으면 하트비트마다 같은 실패를 반복한다.
        SseEmitter emitter = mock(SseEmitter.class);
        doThrow(new IOException("broken pipe")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        registry.register(memberId);
        replaceEmitter(memberId, emitter);

        registry.pushIfPresent(memberId, payload());

        verify(emitter).completeWithError(any());
        verify(container).removeMessageListener(any(MessageListener.class), any(Topic.class));
    }

    @Test
    void 연결이_없는_회원에게_밀면_조용히_무시한다() {
        // 다른 인스턴스에 붙어 있는 회원이다 — 이 인스턴스에선 할 일이 없다.
        registry.pushIfPresent(UUID.randomUUID(), payload());
    }

    /** 모의 emitter를 레지스트리에 심는다 — 전송 실패 경로는 실제 emitter로 만들 수 없다. */
    private void replaceEmitter(UUID memberId, SseEmitter emitter) {
        emitters().put(memberId, emitter);
    }

    private SseEmitter emitterOf(UUID memberId) {
        return emitters().get(memberId);
    }

    @SuppressWarnings("unchecked")
    private java.util.Map<UUID, SseEmitter> emitters() {
        try {
            var field = SseConnectionRegistry.class.getDeclaredField("emitters");
            field.setAccessible(true);
            return (java.util.Map<UUID, SseEmitter>) field.get(registry);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
