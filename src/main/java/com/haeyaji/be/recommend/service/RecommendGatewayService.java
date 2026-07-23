package com.haeyaji.be.recommend.service;

import com.haeyaji.be.common.exception.BusinessException;
import com.haeyaji.be.common.exception.ErrorCode;
import com.haeyaji.be.recommend.client.nlp.NlpClient;
import com.haeyaji.be.recommend.client.nlp.dto.NlpMessageRequest;
import com.haeyaji.be.recommend.client.nlp.dto.NlpMessageResponse;
import com.haeyaji.be.recommend.dto.RecommendMessageRequest;
import com.haeyaji.be.todo.repository.TodoEntity;
import com.haeyaji.be.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * fe 추천 요청을 받아 개인화 프로필·스케줄 맥락을 조립해 nlp로 대행 호출하는 게이트웨이.
 * <p><b>인증 옵셔널</b>: memberId가 있으면 프로필/스케줄로 보강, 없으면(fe JWT 미전송) text/lat/lng만 passthrough.
 * gap 필터는 nlp가 수행하므로 be는 gapMinutes만 정확히 계산해 넘긴다.
 */
@Service
@RequiredArgsConstructor
public class RecommendGatewayService {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final ProfileDistillService profileDistillService;
    private final TodoRepository todoRepository;
    private final NlpClient nlpClient;
    private final Clock clock;

    public NlpMessageResponse recommend(UUID memberId, RecommendMessageRequest request) {
        // 트랜잭션을 두지 않는다: DB 조립(각 read 메서드가 자체 트랜잭션)이 끝나 커넥션이 반납된 뒤에
        // 느린(≤20초) nlp 호출을 한다. open-in-view=false라 여기에 @Transactional을 걸면 LLM 응답 동안
        // Hikari 커넥션을 붙잡아 풀을 고갈시킨다.
        NlpMessageRequest.UserProfile userProfile = null;
        NlpMessageRequest.ScheduleContext scheduleContext = null;
        if (memberId != null) {
            userProfile = profileDistillService.buildUserProfile(memberId, request.mood());
            scheduleContext = buildScheduleContext(memberId);
        }

        NlpMessageRequest nlpRequest = new NlpMessageRequest(
                request.text(), request.lat(), request.lng(), userProfile, scheduleContext);

        NlpMessageResponse response = nlpClient.message(nlpRequest);
        if (response == null) {
            throw new BusinessException(ErrorCode.NLP_UPSTREAM_ERROR);
        }
        return response;
    }

    /**
     * 오늘 시작시간이 있는 할 일로 스케줄 맥락 조립.
     * nextTodoAt/gapMinutes = 지금 이후 가장 이른 시작시간까지, dayTodos = 오늘 시작시간 있는 할 일 전체.
     */
    private NlpMessageRequest.ScheduleContext buildScheduleContext(UUID memberId) {
        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);

        List<TodoEntity> todos = todoRepository
                .findByMemberIdAndTodoDateOrderByPinnedDescSortOrderAscCreatedAtAsc(memberId, today);

        List<NlpMessageRequest.DayTodo> dayTodos = new ArrayList<>();
        LocalTime nextStart = null;
        for (TodoEntity todo : todos) {
            LocalTime start = todo.getStartTime();
            if (start == null) {
                continue; // 시간 없는 할 일은 스케줄 맥락에서 제외
            }
            dayTodos.add(new NlpMessageRequest.DayTodo(todo.getTitle(), start.format(HHMM), null));
            if (start.isAfter(now) && (nextStart == null || start.isBefore(nextStart))) {
                nextStart = start;
            }
        }

        if (dayTodos.isEmpty()) {
            return null; // 오늘 시간표가 없으면 스케줄 맥락 생략
        }

        String nextTodoAt = null;
        Long gapMinutes = null;
        if (nextStart != null) {
            nextTodoAt = LocalDateTime.of(today, nextStart).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            gapMinutes = Duration.between(now, nextStart).toMinutes();
        }
        return new NlpMessageRequest.ScheduleContext(nextTodoAt, gapMinutes, dayTodos);
    }
}
