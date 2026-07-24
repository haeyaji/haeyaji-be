package com.haeyaji.be.profile.dto;

import com.haeyaji.be.profile.domain.MemberPreference;

import java.util.List;

/**
 * 설문 응답 (camelCase). 미저장 유저는 빈 목록/ null로 내려간다.
 */
public record PreferenceResponse(
        List<String> preferredCategories,
        List<String> avoid,
        String vibe,
        String intensity
) {

    public static PreferenceResponse from(MemberPreference p) {
        return new PreferenceResponse(
                p.preferredCategories() != null ? p.preferredCategories() : List.of(),
                p.avoid() != null ? p.avoid() : List.of(),
                p.vibe(),
                p.intensity()
        );
    }
}
