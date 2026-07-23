package com.haeyaji.be.profile.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface MemberCategoryWeightRepository
        extends JpaRepository<MemberCategoryWeightEntity, MemberCategoryWeightId> {

    /** distill: 특정 맥락(날씨·시간대)의 회원 취향을 가중치 내림차순으로. */
    List<MemberCategoryWeightEntity> findByMemberIdAndCtxWeatherAndCtxTimeOfDayOrderByWeightDesc(
            UUID memberId, com.haeyaji.be.profile.domain.CtxWeather ctxWeather,
            com.haeyaji.be.profile.domain.CtxTimeOfDay ctxTimeOfDay);

    /** distill 폴백: 맥락 무시하고 회원 전체 취향. */
    List<MemberCategoryWeightEntity> findByMemberIdOrderByWeightDesc(UUID memberId);

    /** 주1회 decay — 오래된 취향을 x0.9 감쇠(0에 수렴). updated_at은 DDL ON UPDATE로 자동 갱신. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE MemberCategoryWeightEntity w SET w.weight = w.weight * 0.9")
    int decayAll();
}
