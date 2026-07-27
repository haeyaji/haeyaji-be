package com.haeyaji.be.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 온보딩 프로필 설정 (닉네임 + 알림 받을 이메일).
 * <p>{@code email}은 선택 — 보내면 갱신하고, 없으면 기존 값(소셜 로그인에서 받은 주소)을 유지한다.
 * 소셜에서 이메일 제공에 동의하지 않으면 값이 비어 있을 수 있어, 온보딩에서 직접 받아 알림 메일을 보낸다.
 */
public record NicknameUpdateRequest(
        @NotBlank
        @Size(min = 2, max = 10)
        String nickname,

        @Email
        @Size(max = 255)
        String email
) {
}
