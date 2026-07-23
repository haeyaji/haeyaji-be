package com.haeyaji.be.routine.repository;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.DayOfWeek;

/**
 * {@code routine_day.day_of_week}가 DB에 {@code ENUM('MON','TUE','WED','THU','FRI','SAT','SUN')}로
 * 정의돼 있는데, {@link DayOfWeek}의 {@code name()}은 {@code MONDAY} 같은 전체 이름이라
 * 기본 {@code @Enumerated(STRING)}으로 저장하면 DB enum 값과 안 맞아 저장이 깨진다.
 * 앞 3글자가 DB 약어와 그대로 일치해서 substring으로 변환한다.
 */
@Converter(autoApply = false)
public class DayOfWeekConverter implements AttributeConverter<DayOfWeek, String> {

    @Override
    public String convertToDatabaseColumn(DayOfWeek attribute) {
        return attribute == null ? null : attribute.name().substring(0, 3);
    }

    @Override
    public DayOfWeek convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return switch (dbData) {
            case "MON" -> DayOfWeek.MONDAY;
            case "TUE" -> DayOfWeek.TUESDAY;
            case "WED" -> DayOfWeek.WEDNESDAY;
            case "THU" -> DayOfWeek.THURSDAY;
            case "FRI" -> DayOfWeek.FRIDAY;
            case "SAT" -> DayOfWeek.SATURDAY;
            case "SUN" -> DayOfWeek.SUNDAY;
            default -> throw new IllegalArgumentException("알 수 없는 day_of_week 값: " + dbData);
        };
    }
}
