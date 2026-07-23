package com.haeyaji.be.profile.repository;

import com.haeyaji.be.profile.domain.Category;
import com.haeyaji.be.profile.domain.CtxTimeOfDay;
import com.haeyaji.be.profile.domain.CtxWeather;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * {@link MemberCategoryWeightEntity} 복합 PK (member_id, ctx_weather, ctx_time_of_day, category).
 * {@code @IdClass}용 — 필드명/타입이 엔티티의 @Id 필드와 정확히 일치해야 한다.
 */
public class MemberCategoryWeightId implements Serializable {

    private UUID memberId;
    private CtxWeather ctxWeather;
    private CtxTimeOfDay ctxTimeOfDay;
    private Category category;

    protected MemberCategoryWeightId() {
    }

    public MemberCategoryWeightId(UUID memberId, CtxWeather ctxWeather,
                                  CtxTimeOfDay ctxTimeOfDay, Category category) {
        this.memberId = memberId;
        this.ctxWeather = ctxWeather;
        this.ctxTimeOfDay = ctxTimeOfDay;
        this.category = category;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MemberCategoryWeightId that)) {
            return false;
        }
        return Objects.equals(memberId, that.memberId)
                && ctxWeather == that.ctxWeather
                && ctxTimeOfDay == that.ctxTimeOfDay
                && category == that.category;
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberId, ctxWeather, ctxTimeOfDay, category);
    }
}
