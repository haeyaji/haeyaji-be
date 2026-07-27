package com.haeyaji.be.common.cache;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 테스트용 캐시 저장소 — Redis 없이 캐시 동작(히트/미스)을 그대로 검증하기 위한 인메모리 대역.
 * <p>TTL은 무시한다(테스트는 만료를 시간으로 기다리지 않고, 필요하면 {@link #clear()}로 비운다).
 */
public class InMemoryCacheStore extends RedisCacheStore {

    private final Map<String, Object> store = new HashMap<>();

    public InMemoryCacheStore() {
        super(null, null); // 상위는 Redis 연결을 쓰지 않는다 — 모든 접근을 아래에서 가로챈다.
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        Object value = store.get(key);
        return value == null ? null : type.cast(value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getParametric(String key, Class<?> rawType, Class<?> elementType) {
        return (T) store.get(key);
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        store.put(key, value);
    }

    public void clear() {
        store.clear();
    }
}
