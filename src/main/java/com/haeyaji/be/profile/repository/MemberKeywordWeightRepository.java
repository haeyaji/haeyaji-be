package com.haeyaji.be.profile.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MemberKeywordWeightRepository
        extends JpaRepository<MemberKeywordWeightEntity, MemberKeywordWeightId> {

    /** distill: 회원 키워드 취향 상위. */
    List<MemberKeywordWeightEntity> findByMemberIdOrderByWeightDesc(UUID memberId);

    /** category_weight와 동일한 원자적 UPSERT. */
    @Modifying
    @Query(value = """
            INSERT INTO member_keyword_weight (member_id, keyword, weight, updated_at)
            VALUES (:memberId, :keyword, :delta, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE weight = weight + :delta, updated_at = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    void upsertWeight(@Param("memberId") byte[] memberId,
                      @Param("keyword") String keyword,
                      @Param("delta") double delta);

    /** 주1회 decay — category_weight와 동일. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE MemberKeywordWeightEntity w SET w.weight = w.weight * 0.9, w.updatedAt = CURRENT_TIMESTAMP")
    int decayAll();
}
