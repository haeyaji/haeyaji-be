package com.haeyaji.be.weather.infrastructure.kma;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 초단기실황·초단기예보 발표시각(base_date/base_time) 계산.
 * <ul>
 *   <li>실황(getUltraSrtNcst): 매시 정시 생성, 약 10분 후 제공</li>
 *   <li>예보(getUltraSrtFcst): 매시 30분 발표, 약 45분 후 제공</li>
 * </ul>
 * 제공 버퍼만큼 시각을 되돌린 뒤 해당 발표시각을 고른다.
 */
final class KmaUltraBaseTime {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int NCST_AVAILABLE_AFTER_MIN = 10;
    private static final int FCST_AVAILABLE_AFTER_MIN = 45;

    private KmaUltraBaseTime() {
    }

    record BaseTime(String baseDate, String baseTime) {
    }

    /** 초단기실황: 매시 정시 발표분 중 조회 가능한 최신. */
    static BaseTime nowcast(LocalDateTime now) {
        LocalDateTime t = now.minusMinutes(NCST_AVAILABLE_AFTER_MIN);
        return new BaseTime(t.format(DATE), "%02d00".formatted(t.getHour()));
    }

    /** 초단기예보: 매시 30분 발표분 중 조회 가능한 최신. */
    static BaseTime forecast(LocalDateTime now) {
        LocalDateTime t = now.minusMinutes(FCST_AVAILABLE_AFTER_MIN);
        return new BaseTime(t.format(DATE), "%02d30".formatted(t.getHour()));
    }
}
