package com.haeyaji.be.recommend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.haeyaji.be.profile.domain.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * fe → be 추천 게이트웨이 요청. 필수 {@code text/lat/lng}. {@code selectedCategory}가 있으면 2단계(장소).
 * <p>프로필(선호/회피/강도/최근선택)·스케줄 맥락은 be가 DB에서 조립하고, 아래 대화 맥락
 * ({@code weather/mood/timeOfDay/weekday/radiusM/history})은 fe가 보내면 be가 nlp로 그대로 통과시킨다(품질 보강).
 * <p>{@code /message}는 permitAll이고 매 호출이 nlp LLM을 트리거하므로 {@code text}에 길이 상한을 둔다(레이트리밋은 후속).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RecommendMessageRequest(
        @NotBlank @Size(max = 500) String text,
        @NotNull Double lat,
        @NotNull Double lng,
        Category selectedCategory,
        String weather,
        String mood,
        String timeOfDay,
        String weekday,
        Integer radiusM,
        @Size(max = 20) List<Object> history
) {
}
