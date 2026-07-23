package com.haeyaji.be.profile.dto;

import com.haeyaji.be.profile.domain.Category;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 카테고리 선택 피드백. nlp가 카테고리 후보를 제시하고 사용자가 하나 고르는 순간 fe가 1회 전송.
 * <p>be가 신호를 분배한다: {@code selected} +2, {@code shown} 중 selected 제외 각 −0.05,
 * {@code keywords}(있으면) 각 +2(키워드 축 학습).
 *
 * @param shown    이번에 뜬 카테고리 전체(2~4개). selected 포함.
 * @param selected 고른 카테고리. 반드시 shown 안에 있어야 함.
 * @param keywords nlp가 이 선택에서 뽑은 세부 키워드(선택). 없으면 빈 목록/null.
 */
public record ChoiceFeedbackRequest(
        @NotEmpty @Size(max = 10) List<Category> shown,
        @NotNull Category selected,
        @Size(max = 20) List<@Size(max = 50) String> keywords
) {
}
