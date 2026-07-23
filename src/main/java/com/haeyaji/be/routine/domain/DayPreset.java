package com.haeyaji.be.routine.domain;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Set;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;

/**
 * 루틴 반복요일 프리셋 (ROUT-2). 등록 시 입력이 아니라, 저장된 요일 조합을 보고 응답 시 분류해서 내려준다
 * (프리셋 버튼 선택 계산은 fe 담당 — be는 최종 요일 Set만 받고, 어떤 프리셋에 해당하는지만 판정).
 */
public enum DayPreset {
    DAILY,
    WEEKDAY,
    WEEKEND,
    CUSTOM;

    private static final Set<DayOfWeek> WEEKDAYS = EnumSet.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY);
    private static final Set<DayOfWeek> WEEKEND_DAYS = EnumSet.of(SATURDAY, SUNDAY);

    public static DayPreset from(Set<DayOfWeek> days) {
        if (days.size() == 7) {
            return DAILY;
        }
        if (days.equals(WEEKDAYS)) {
            return WEEKDAY;
        }
        if (days.equals(WEEKEND_DAYS)) {
            return WEEKEND;
        }
        return CUSTOM;
    }
}
