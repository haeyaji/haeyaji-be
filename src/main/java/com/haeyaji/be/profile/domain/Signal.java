package com.haeyaji.be.profile.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 취향 학습 신호와 가중치 델타.
 * <p><b>카테고리 선택 시점 학습</b>: nlp가 카테고리 후보를 제시하고 사용자가 고르는 순간 학습한다.
 * <ul>
 *   <li>{@link #SELECTED} +2 : 고른 카테고리(강한 긍정). 키워드 학습에도 재사용.</li>
 *   <li>{@link #NOT_SELECTED} −0.05 : 같이 떴는데 안 고른 카테고리(아주 약한 감점).
 *       반복 노출+미선택이 누적되면 서서히 추천에서 밀려나 "싫은데 계속 추천"을 방지.</li>
 * </ul>
 * 명시적 "싫어" 버튼·큰 벌점은 없다(챗 UX에 안 맞음). "다른거 추천"은 장소 단계에서 키워드로만 처리.
 * 누적 후 주1회 x0.9 decay.
 */
@Getter
@RequiredArgsConstructor
public enum Signal {
    SELECTED(2.0),
    NOT_SELECTED(-0.05);

    private final double delta;
}
