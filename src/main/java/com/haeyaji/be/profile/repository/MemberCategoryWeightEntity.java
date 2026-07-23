package com.haeyaji.be.profile.repository;

import com.haeyaji.be.profile.domain.Category;
import com.haeyaji.be.profile.domain.CtxTimeOfDay;
import com.haeyaji.be.profile.domain.CtxWeather;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 카테고리 가중치 테이블(member_category_weight) 매핑.
 * <p>복합 PK (memberId, ctxWeather, ctxTimeOfDay, category) — {@link MemberCategoryWeightId}.
 * 맥락(날씨·시간대)별로 카테고리 취향을 분리 누적한다. member_id는 느슨한 UUID 컬럼(연관관계 아님).
 */
@Entity
@Table(name = "member_category_weight")
@IdClass(MemberCategoryWeightId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberCategoryWeightEntity {

    @Id
    @Column(name = "member_id")
    private UUID memberId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "ctx_weather")
    private CtxWeather ctxWeather;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "ctx_time_of_day")
    private CtxTimeOfDay ctxTimeOfDay;

    @Id
    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(nullable = false)
    private double weight;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 쓰기(누적)는 repository의 원자적 UPSERT로 처리한다. 엔티티는 distill 읽기 매핑 전용.
}
