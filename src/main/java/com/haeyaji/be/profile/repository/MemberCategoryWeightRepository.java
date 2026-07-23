package com.haeyaji.be.profile.repository;

import com.haeyaji.be.profile.domain.CtxTimeOfDay;
import com.haeyaji.be.profile.domain.CtxWeather;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MemberCategoryWeightRepository
        extends JpaRepository<MemberCategoryWeightEntity, MemberCategoryWeightId> {

    /** distill: 특정 맥락(날씨·시간대)의 회원 취향을 가중치 내림차순으로. */
    List<MemberCategoryWeightEntity> findByMemberIdAndCtxWeatherAndCtxTimeOfDayOrderByWeightDesc(
            UUID memberId, CtxWeather ctxWeather, CtxTimeOfDay ctxTimeOfDay);

    /** distill 폴백: 맥락 무시하고 회원 전체 취향. */
    List<MemberCategoryWeightEntity> findByMemberIdOrderByWeightDesc(UUID memberId);

    /**
     * 신호 델타를 원자적으로 누적(UPSERT). 없으면 delta로 생성, 있으면 weight += delta.
     * findById→save의 read-modify-write(동시 최초삽입 시 PK 위반 500·lost-update)를 DB 원자연산으로 제거한다.
     * memberId는 binary(16) 컬럼이라 byte[16](big-endian)로, enum은 name() 문자열로 바인딩한다.
     */
    @Modifying
    @Query(value = """
            INSERT INTO member_category_weight
                (member_id, ctx_weather, ctx_time_of_day, category, weight, updated_at)
            VALUES (:memberId, :ctxWeather, :ctxTimeOfDay, :category, :delta, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE weight = weight + :delta, updated_at = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    void upsertWeight(@Param("memberId") byte[] memberId,
                      @Param("ctxWeather") String ctxWeather,
                      @Param("ctxTimeOfDay") String ctxTimeOfDay,
                      @Param("category") String category,
                      @Param("delta") double delta);

    /** 주1회 decay — 오래된 취향을 x0.9 감쇠(0에 수렴). bulk update는 @UpdateTimestamp를 안 타므로 updated_at을 명시 갱신. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE MemberCategoryWeightEntity w SET w.weight = w.weight * 0.9, w.updatedAt = CURRENT_TIMESTAMP")
    int decayAll();
}
