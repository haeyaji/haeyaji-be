package com.haeyaji.be.profile.domain;

import java.time.LocalTime;

/**
 * 가중치 학습 맥락 — 시간대. DDL {@code member_category_weight.ctx_time_of_day} ENUM과 1:1.
 * 같은 취향도 아침/저녁이 다르므로 시간대별로 가중치를 분리 누적한다.
 */
public enum CtxTimeOfDay {
    MORNING,
    AFTERNOON,
    EVENING,
    NIGHT;

    /**
     * 시각 → 시간대 매핑. 아침 05~11, 오후 12~16, 저녁 17~20, 나머지(21~04) 밤.
     * 신호 발생 시각(KST 기준 Clock)으로 맥락을 정한다.
     */
    public static CtxTimeOfDay from(LocalTime time) {
        int h = time.getHour();
        if (h >= 5 && h < 12) {
            return MORNING;
        }
        if (h >= 12 && h < 17) {
            return AFTERNOON;
        }
        if (h >= 17 && h < 21) {
            return EVENING;
        }
        return NIGHT;
    }
}
