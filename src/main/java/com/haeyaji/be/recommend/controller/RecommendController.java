package com.haeyaji.be.recommend.controller;

import com.haeyaji.be.common.response.ApiResponse;
import com.haeyaji.be.common.response.SuccessCode;
import com.haeyaji.be.member.oauth.CustomUserDetails;
import com.haeyaji.be.recommend.client.nlp.dto.NlpMessageResponse;
import com.haeyaji.be.recommend.dto.RecommendMessageRequest;
import com.haeyaji.be.recommend.service.RecommendGatewayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 추천 게이트웨이(FR-7). fe → be → nlp 대행 호출(fe→nlp 직접호출 차단).
 *
 * <pre>
 * POST /api/message   {text, lat, lng, mood?} → nlp {intent, reply, todos[], options[], actions[]}
 * </pre>
 *
 * <p>permitAll(인증 옵셔널): 인증되면 개인화 프로필·스케줄로 보강, 미인증(fe JWT 미전송)이면 passthrough.
 */
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendGatewayService recommendGatewayService;

    @PostMapping
    public ApiResponse<NlpMessageResponse> message(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody RecommendMessageRequest request
    ) {
        UUID memberId = userDetails != null ? userDetails.getMemberId() : null;
        NlpMessageResponse response = recommendGatewayService.recommend(memberId, request);
        return ApiResponse.of(response, SuccessCode.GET_SUCCESS);
    }
}
