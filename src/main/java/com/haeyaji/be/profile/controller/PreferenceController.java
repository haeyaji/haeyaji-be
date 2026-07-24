package com.haeyaji.be.profile.controller;

import com.haeyaji.be.common.response.ApiResponse;
import com.haeyaji.be.common.response.SuccessCode;
import com.haeyaji.be.member.oauth.CustomUserDetails;
import com.haeyaji.be.profile.dto.PreferenceRequest;
import com.haeyaji.be.profile.dto.PreferenceResponse;
import com.haeyaji.be.profile.service.PreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 개인화 설문 preference (FR-7). 인증 필수(SecurityConfig permitAll 목록 밖).
 *
 * <pre>
 * POST /api/preferences   설문 4축 저장(upsert)
 * GET  /api/preferences   내 설문 조회
 * </pre>
 */
@RestController
@RequestMapping("/preferences")
@RequiredArgsConstructor
public class PreferenceController {

    private final PreferenceService preferenceService;

    @PostMapping
    public ApiResponse<PreferenceResponse> save(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PreferenceRequest request
    ) {
        PreferenceResponse saved = PreferenceResponse.from(
                preferenceService.save(userDetails.getMemberId(), request));
        return ApiResponse.of(saved, SuccessCode.PUT_SUCCESS);
    }

    @GetMapping
    public ApiResponse<PreferenceResponse> get(@AuthenticationPrincipal CustomUserDetails userDetails) {
        PreferenceResponse pref = PreferenceResponse.from(
                preferenceService.get(userDetails.getMemberId()));
        return ApiResponse.of(pref, SuccessCode.GET_SUCCESS);
    }
}
