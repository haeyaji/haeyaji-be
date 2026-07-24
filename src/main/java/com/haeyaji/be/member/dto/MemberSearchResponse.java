package com.haeyaji.be.member.dto;

import com.haeyaji.be.member.domain.Member;

import java.util.UUID;

/**
 * 닉네임으로 다른 회원을 검색할 때 쓰는 응답 — MemberResponse(내 정보용)와 달리
 * email/role/status 등 개인정보는 담지 않고, 친구 요청에 필요한 최소 정보(id, nickname)만 노출한다.
 */
public record MemberSearchResponse(
        UUID id,
        String nickname
) {

    public static MemberSearchResponse from(Member member) {
        return new MemberSearchResponse(member.getId(), member.getNickname());
    }
}
