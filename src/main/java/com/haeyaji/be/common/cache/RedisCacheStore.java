package com.haeyaji.be.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.type.TypeFactory;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 외부 API 응답 캐시 저장소(Redis).
 *
 * <p>인스턴스 메모리 대신 Redis에 두는 이유:
 * <ul>
 *   <li><b>일일 호출 상한</b> — data.go.kr(미세먼지·자외선)은 계정당 하루 호출 수가 정해져 있다.
 *       인스턴스마다 따로 캐시하면 인스턴스 수만큼 상류를 때린다.</li>
 *   <li><b>재시작 내성</b> — 배포·재시작 때마다 캐시가 비면 그 직후 상류 호출이 몰린다.</li>
 *   <li><b>메모리</b> — 좌표별 캐시가 앱 힙을 잠식하지 않는다. TTL 만료도 Redis가 알아서 처리해
 *       직접 만들던 축출(size 초과 시 전체 삭제) 로직이 필요 없다.</li>
 * </ul>
 *
 * <p><b>캐시는 부가 기능이다</b> — Redis가 죽어도 서비스는 계속돼야 하므로, 조회·저장 실패는
 * 모두 흡수하고 상류 호출로 자연스럽게 넘어간다(캐시 미스와 동일하게 취급).
 *
 * <p>값은 JSON으로 저장한다. 타입 정보를 함께 넣지 않으므로 읽을 때 기대 타입을 명시해야 한다.
 */
@Slf4j
@Component
public class RedisCacheStore {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisCacheStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /**
     * 캐시에서 읽고, 없으면 {@code loader}로 만들어 저장한 뒤 돌려준다.
     * {@code loader}가 {@code null}을 주면 저장하지 않는다(실패 응답을 캐시하지 않기 위함).
     */
    public <T> T getOrLoad(String key, Class<T> type, Duration ttl, Supplier<T> loader) {
        T cached = get(key, type);
        if (cached != null) {
            return cached;
        }
        T loaded = loader.get();
        if (loaded != null) {
            put(key, loaded, ttl);
        }
        return loaded;
    }

    /** 제네릭 컨테이너(List 등)용. 예: {@code getOrLoad(key, List.class, Station.class, ttl, loader)}. */
    public <T> T getOrLoad(String key, Class<?> rawType, Class<?> elementType, Duration ttl, Supplier<T> loader) {
        T cached = getParametric(key, rawType, elementType);
        if (cached != null) {
            return cached;
        }
        T loaded = loader.get();
        if (loaded != null) {
            put(key, loaded, ttl);
        }
        return loaded;
    }

    public <T> T get(String key, Class<T> type) {
        try {
            String raw = redis.opsForValue().get(key);
            return raw == null ? null : objectMapper.readValue(raw, type);
        } catch (Exception e) {
            // 캐시는 부가 기능 — 못 읽으면 미스로 간주하고 상류에서 다시 받는다.
            log.debug("캐시 조회 실패(미스로 처리): key={} err={}", key, e.toString());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getParametric(String key, Class<?> rawType, Class<?> elementType) {
        try {
            String raw = redis.opsForValue().get(key);
            if (raw == null) {
                return null;
            }
            TypeFactory factory = objectMapper.getTypeFactory();
            return (T) objectMapper.readValue(raw, factory.constructCollectionType(
                    (Class<? extends java.util.Collection>) rawType, elementType));
        } catch (Exception e) {
            log.debug("캐시 조회 실패(미스로 처리): key={} err={}", key, e.toString());
            return null;
        }
    }

    public void put(String key, Object value, Duration ttl) {
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception e) {
            // 저장 못 해도 이번 요청은 이미 값을 갖고 있다 — 다음 요청이 상류를 한 번 더 부를 뿐.
            log.debug("캐시 저장 실패(무시): key={} err={}", key, e.toString());
        }
    }
}
