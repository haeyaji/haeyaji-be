package com.haeyaji.be.profile.controller;

import com.haeyaji.be.common.response.ApiResponse;
import com.haeyaji.be.common.response.SuccessCode;
import com.haeyaji.be.member.oauth.CustomUserDetails;
import com.haeyaji.be.profile.dto.FeedbackRequest;
import com.haeyaji.be.profile.service.WeightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 추천 피드백 신호 수집(FR-7). 인증 필수(SecurityConfig permitAll 밖).
 *
 * <pre>
 * POST /api/recommend/feedback   {category, signal} → 맥락별 카테고리 가중치 누적(fire-and-forget)
 * </pre>
 */
@RestController
@RequestMapping("/recommend")
@RequiredArgsConstructor
public class FeedbackController {

    private final WeightService weightService;

    @PostMapping("/feedback")
    public ApiResponse<Void> feedback(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody FeedbackRequest request
    ) {
        weightService.applyCategorySignal(
                userDetails.getMemberId(), request.category(), request.signal());
        return ApiResponse.of(null, SuccessCode.POST_SUCCESS);
    }
}
