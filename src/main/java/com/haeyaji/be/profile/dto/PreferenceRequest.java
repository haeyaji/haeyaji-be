package com.haeyaji.be.profile.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 설문 저장 요청. 4축 모두 선택/스킵 가능(콜드스타트) — 전부 optional.
 * 값은 온보딩 선택지 문자열 그대로(한글). fe {@code POST /preferences} 계약과 일치.
 */
public record PreferenceRequest(
        List<String> preferredCategories,
        List<String> avoid,
        @Size(max = 20) String vibe,
        @Size(max = 20) String intensity
) {
}
