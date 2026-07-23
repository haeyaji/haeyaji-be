package com.haeyaji.be.member.repository;

import com.haeyaji.be.member.domain.SocialType;
import com.haeyaji.be.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberRepository extends JpaRepository<Member, UUID> {
    Optional<Member> findBySocialTypeAndSocialTypeId(SocialType socialType, String socialTypeId);
}
