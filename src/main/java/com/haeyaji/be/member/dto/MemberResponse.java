package com.haeyaji.be.member.dto;

import com.haeyaji.be.member.domain.Member;
import com.haeyaji.be.member.domain.MemberRole;
import com.haeyaji.be.member.domain.MemberStatus;

import java.time.LocalDateTime;

public record MemberResponse(
        String nickname,
        String email,
        MemberRole role,
        MemberStatus status,
        LocalDateTime createdAt
) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getNickname(),
                member.getEmail(),
                member.getRole(),
                member.getStatus(),
                member.getCreatedAt()
        );
    }
}
