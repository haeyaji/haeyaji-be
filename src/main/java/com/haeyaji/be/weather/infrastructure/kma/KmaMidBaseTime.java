package com.haeyaji.be.weather.infrastructure.kma;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 중기예보 발표시각(tmFc) 계산. 중기예보는 매일 06·18시 발표.
 *
 * <p>일관된 커버리지를 위해 <b>가장 최근의 06시 발표</b>만 사용한다.
 * (06시 발표는 +4~+10일 제공, 18시 발표는 +5~+10일만 제공 → 06시 고정이 +4일 갭을 막음)
 * 따라서 tmFc 발표일 기준 n일 후 = wf{n} 이며, n은 항상 4~10 범위로 들어온다.
 */
final class KmaMidBaseTime {

    // 06시 발표가 조회 가능해지는 시각(약간의 버퍼). 이 전이면 전날 06시 발표 사용.
    private static final int AVAILABLE_HOUR = 7;
    private static final DateTimeFormatter TMFC = DateTimeFormatter.ofPattern("yyyyMMdd'0600'");

    private KmaMidBaseTime() {
    }

    /** 발표시각 tmFc 문자열(yyyyMMdd0600). */
    static String resolveTmFc(LocalDateTime now) {
        return baseDate(now).format(TMFC);
    }

    /** tmFc 발표일(=기준일). 발효일과의 일수 차이가 wf 인덱스가 된다. */
    static LocalDate baseDate(LocalDateTime now) {
        return now.getHour() >= AVAILABLE_HOUR ? now.toLocalDate() : now.toLocalDate().minusDays(1);
    }
}
