package com.haeyaji.be.profile.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface MemberKeywordWeightRepository
        extends JpaRepository<MemberKeywordWeightEntity, MemberKeywordWeightId> {

    /** distill: 회원 키워드 취향 상위. */
    List<MemberKeywordWeightEntity> findByMemberIdOrderByWeightDesc(UUID memberId);

    /** 주1회 decay — category_weight와 동일. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE MemberKeywordWeightEntity w SET w.weight = w.weight * 0.9")
    int decayAll();
}
