package com.haeyaji.be.recommend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.haeyaji.be.profile.domain.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * fe → be 추천 게이트웨이 요청. fe는 {@code text/lat/lng}와 {@code mood}(=vibe 1축)만 보내고,
 * 나머지 프로필(선호/회피/강도/최근선택)·스케줄 맥락은 be가 DB에서 조립한다.
 * <p>fe가 weather/timeOfDay/weekday/history 등을 더 보내도 400 나지 않게 미지정 필드는 무시.
 * <p>{@code /message}는 permitAll이고 매 호출이 nlp LLM을 트리거하므로, {@code text}에 길이 상한을 둬
 * 대용량 본문으로 인한 비용 증폭·본문 파싱 부하를 1차 차단한다(레이트리밋은 후속 과제).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RecommendMessageRequest(
        @NotBlank @Size(max = 500) String text,
        @NotNull Double lat,
        @NotNull Double lng,
        String mood,
        Category selectedCategory
) {
}
