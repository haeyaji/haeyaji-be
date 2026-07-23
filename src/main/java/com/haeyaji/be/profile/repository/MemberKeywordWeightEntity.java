package com.haeyaji.be.profile.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 키워드 가중치 테이블(member_keyword_weight) 매핑.
 * <p>복합 PK (memberId, keyword) — {@link MemberKeywordWeightId}. keyword는 nlp 자유 검색어(방탈출 등),
 * enum이 아니라 varchar. category_weight와 동일 공식·decay. (신호 주입은 커밋3 게이트웨이에서 연결)
 */
@Entity
@Table(name = "member_keyword_weight")
@IdClass(MemberKeywordWeightId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberKeywordWeightEntity {

    @Id
    @Column(name = "member_id")
    private UUID memberId;

    @Id
    @Column(length = 50)
    private String keyword;

    @Column(nullable = false)
    private double weight;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static MemberKeywordWeightEntity create(UUID memberId, String keyword) {
        MemberKeywordWeightEntity entity = new MemberKeywordWeightEntity();
        entity.memberId = memberId;
        entity.keyword = keyword;
        entity.weight = 0;
        return entity;
    }

    public void addDelta(double delta) {
        this.weight += delta;
    }
}
