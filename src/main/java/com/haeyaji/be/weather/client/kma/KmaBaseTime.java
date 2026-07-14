package com.haeyaji.be.weather.client.kma;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 단기예보(getVilageFcst) 발표시각(base_date/base_time) 계산.
 * 발표는 매일 02·05·08·11·14·17·20·23시(8회), API 반영은 발표 후 약 10분.
 * 현재 시각 기준으로 이미 반영된 가장 최근 발표시각을 고른다.
 */
final class KmaBaseTime {

    private static final int[] BASE_HOURS = {2, 5, 8, 11, 14, 17, 20, 23};
    private static final int AVAILABLE_AFTER_MIN = 10;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private KmaBaseTime() {
    }

    record BaseTime(String baseDate, String baseTime) {
    }

    static BaseTime resolve(LocalDateTime now) {
        // 발표시각 + 10분이 지나야 조회 가능 → 10분 이전 데이터 기준으로 판단
        LocalDateTime effective = now.minusMinutes(AVAILABLE_AFTER_MIN);
        int hour = effective.getHour();

        int chosen = -1;
        for (int h : BASE_HOURS) {
            if (h <= hour) {
                chosen = h;
            }
        }
        if (chosen == -1) {
            // 02시 발표 이전 → 전날 23시 발표 사용
            LocalDateTime prev = effective.minusDays(1);
            return new BaseTime(prev.format(DATE), "2300");
        }
        return new BaseTime(effective.format(DATE), "%02d00".formatted(chosen));
    }
}
