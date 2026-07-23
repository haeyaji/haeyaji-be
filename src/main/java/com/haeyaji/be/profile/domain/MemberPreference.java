package com.haeyaji.be.profile.domain;

import java.util.List;
import java.util.UUID;

/**
 * 개인화 설문(선언적 취향) 도메인 (FR-7). member와 1:1.
 * 전부 선택/스킵 가능한 콜드스타트 신호 — 행동 가중치가 쌓이면 뒤로 밀린다.
 * 값은 nlp UserProfile로 그대로 흘러가는 자유 텍스트(한글 선택지 문자열).
 */
public record MemberPreference(
        UUID memberId,
        List<String> preferredCategories,
        List<String> avoid,
        String vibe,
        String intensity
) {
}
