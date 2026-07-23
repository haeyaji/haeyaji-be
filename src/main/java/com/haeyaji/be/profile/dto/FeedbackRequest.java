package com.haeyaji.be.profile.dto;

import com.haeyaji.be.profile.domain.Category;
import com.haeyaji.be.profile.domain.Signal;
import jakarta.validation.constraints.NotNull;

/**
 * 추천 피드백 신호. fe가 추천 무시 시 {@code {category, signal:"IGNORED"}}로 호출(fire-and-forget).
 * 긍정 신호(SELECTED/ADD)도 같은 엔드포인트로 받을 수 있다. 값은 6종 코드값.
 */
public record FeedbackRequest(
        @NotNull Category category,
        @NotNull Signal signal
) {
}
