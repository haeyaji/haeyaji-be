package com.haeyaji.be.member.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Refresh Token을 Redis에 저장. key = "refreshToken:{memberId}", TTL = 만료시간.
 * JPA가 아니라 Redis를 쓰는 이유: 만료 처리를 TTL로 자동화하고, 재발급/로그아웃 때만 조회하면 되기 때문.
 *
 * auth-demo(kr.co.ureca.authdemo.jwt.RefreshTokenRepository, Long userId 버전)에
 * 이미 구현된 동일 클래스가 있으니 그걸 참고해서 UUID로만 바꿔주면 됨.
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private static final String KEY_PREFIX = "refreshToken:";

    private final StringRedisTemplate redisTemplate;

    public void save(UUID memberId, String refreshToken, Duration ttl) {
        redisTemplate.opsForValue().set(KEY_PREFIX + memberId, refreshToken, ttl);
    }

    public Optional<String> findByMemberId(UUID memberId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + memberId));
    }

    // 로그아웃 시 사용
    public void deleteByMemberId(UUID memberId) {
        redisTemplate.delete(KEY_PREFIX + memberId);
    }
}
