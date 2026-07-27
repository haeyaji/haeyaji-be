package com.haeyaji.be.notification.scheduler;

import com.haeyaji.be.notification.domain.NotificationCategory;
import com.haeyaji.be.notification.domain.NotificationType;
import com.haeyaji.be.notification.service.NotificationService;
import com.haeyaji.be.todo.domain.InviteStatus;
import com.haeyaji.be.todo.domain.TodoStatus;
import com.haeyaji.be.todo.repository.TodoEntity;
import com.haeyaji.be.todo.repository.TodoParticipantEntity;
import com.haeyaji.be.todo.repository.TodoParticipantRepository;
import com.haeyaji.be.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * start_time이 있는 할 일 시작 10분 전, 소유자 + 공유 수락(ACCEPTED)한 참여자 전원에게
 * TODO_REMINDER를 발송한다. "공유자냐 아니냐"는 별도로 구분하지 않고 todoId 기준으로 이 할 일에
 * 접근 권한이 있는 전원(소유자 + ACCEPTED 참여자)에게 동일하게 보낸다 — todo_participant는 owner
 * 행을 두지 않는 별도 테이블이고, PENDING(아직 수락 전)·REJECTED(거절) 참여자는 이 할 일에 대한
 * 접근권이 없으므로 제외한다 (TodoService.updateTodo의 recipients 계산과 동일 패턴).
 * meeting/todo 도메인 서비스는 notification을 모르게 하는 이벤트 리스너 컨벤션과 반대로, 스케줄러는
 * 걸어들 이벤트가 없어(도메인 액션이 없음) notification 쪽이 todo 리포지터리를 직접 참조한다 —
 * TodoEventListener가 닉네임 조회용으로 MemberRepository를 참조하는 것과 같은 방향의 의존.
 * MeetingReminderScheduler와 동일하게 등치 비교(자정 경계 안전) + IDEMPOTENT_TYPES 이중 안전장치.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TodoReminderScheduler {

    private static final ZoneId SCHEDULE_ZONE = ZoneId.of("Asia/Seoul");

    private final TodoRepository todoRepository;
    private final TodoParticipantRepository todoParticipantRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void remindUpcomingTodos() {
        LocalDateTime target = LocalDateTime.now(clock.withZone(SCHEDULE_ZONE))
                .truncatedTo(ChronoUnit.MINUTES)
                .plusMinutes(10);
        LocalDate targetDate = target.toLocalDate();
        LocalTime targetTime = target.toLocalTime();

        List<TodoEntity> dueTodos = todoRepository
                .findByTodoDateAndStartTimeAndStatusNot(targetDate, targetTime, TodoStatus.DONE);

        for (TodoEntity todo : dueTodos) {
            List<UUID> recipients = new ArrayList<>();
            recipients.add(todo.getMemberId());
            todoParticipantRepository.findByTodoId(todo.getId()).stream()
                    .filter(p -> p.getInviteStatus() == InviteStatus.ACCEPTED)
                    .map(TodoParticipantEntity::getMemberId)
                    .forEach(recipients::add);

            String body = "할 일이 10분 후 시작됩니다: " + todo.getStartTime();
            for (UUID memberId : recipients) {
                try {
                    notificationService.sendSystem(
                            memberId,
                            NotificationCategory.TODO, NotificationType.TODO_REMINDER,
                            todo.getTitle(), body, todo.getId()
                    );
                } catch (Exception e) {
                    log.error("TODO_REMINDER 알림 발송 실패: todoId={}, memberId={}", todo.getId(), memberId, e);
                }
            }
        }
    }
}
