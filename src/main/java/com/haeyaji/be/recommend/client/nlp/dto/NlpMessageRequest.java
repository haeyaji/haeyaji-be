package com.haeyaji.be.recommend.client.nlp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.haeyaji.be.profile.domain.Category;

import java.util.List;

/**
 * nlp {@code POST /api/message} 요청 미러(camelCase, nlp 계약과 1:1).
 * <p>필수 {@code text/lat/lng}. {@code selectedCategory}가 있으면 2단계(그 카테고리 안 장소 추천), 없으면 1단계(카테고리 후보).
 * {@code userProfile}·{@code scheduleContext}는 be가 DB에서 조립해 실어보내며, 미인증 요청에선 생략된다(NON_NULL).
 * gap 필터는 nlp가 수행하므로 be는 {@code gapMinutes}만 정확히 넘긴다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NlpMessageRequest(
        String text,
        double lat,
        double lng,
        Category selectedCategory,
        String weather,
        String mood,
        String timeOfDay,
        String weekday,
        Integer radiusM,
        List<Object> history,
        UserProfile userProfile,
        ScheduleContext scheduleContext
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UserProfile(
            List<String> preferredCategories,
            String vibe,
            String intensity,
            List<String> avoid,
            List<String> recentSelections
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ScheduleContext(
            String nextTodoAt,
            Long gapMinutes,
            List<DayTodo> dayTodos
    ) {
    }

    /** startTime "HH:mm". todo엔 종료시간이 없어 endTime은 항상 null(생략). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DayTodo(
            String title,
            String startTime,
            String endTime
    ) {
    }
}
