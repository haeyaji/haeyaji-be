package com.haeyaji.be.profile.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 취향 학습 신호와 가중치 델타.
 * <p><b>선택 시점 학습</b>: 사용자가 무엇을 고르/무시했는지로 배운다(완료 DONE은 신호에서 제외 —
 * todo에 category가 없기도 하고, 완료는 취향이 아니라 이행 여부라서).
 * <ul>
 *   <li>{@link #SELECTED} +2 : 추천 결과를 골라 담음(가장 강한 긍정)</li>
 *   <li>{@link #ADD} +1 : 장소를 직접 할 일로 추가</li>
 *   <li>{@link #IGNORED} −0.5 : 추천을 무시/스킵(약한 부정)</li>
 * </ul>
 * 누적 후 주1회 x0.9 decay(오래된 취향 감쇠)한다.
 */
@Getter
@RequiredArgsConstructor
public enum Signal {
    SELECTED(2.0),
    ADD(1.0),
    IGNORED(-0.5);

    private final double delta;
}
