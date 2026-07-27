package com.haeyaji.be.notification.scheduler;

import com.haeyaji.be.meeting.domain.MeetingStatus;
import com.haeyaji.be.meeting.repository.MeetingEntity;
import com.haeyaji.be.meeting.repository.MeetingParticipantEntity;
import com.haeyaji.be.meeting.repository.MeetingParticipantRepository;
import com.haeyaji.be.meeting.repository.MeetingRepository;
import com.haeyaji.be.notification.domain.NotificationCategory;
import com.haeyaji.be.notification.domain.NotificationType;
import com.haeyaji.be.notification.service.NotificationService;
import com.haeyaji.be.todo.domain.InviteStatus;
import com.haeyaji.be.todo.domain.TodoSource;
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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 다가온 일정을 미리 알린다 — 할 일 시작 30분 전, 확정된 약속 1시간 전.
 *
 * <p>주기 실행이라 같은 일정을 여러 번 집게 되는데, 중복 발송은
 * {@code notification}의 유니크 제약 {@code (member_id, type, ref_id)}가 막는다
 * (TODO_REMINDER·MEETING_REMINDER는 멱등 대상). 그래서 스케줄러는 "창(window)에 걸린 것"만
 * 단순히 훑으면 되고, 어디까지 보냈는지 따로 기록하지 않는다.
 *
 * <p>알림 시각을 "정확히 30분 전"이 아니라 "30분 안"으로 잡는 이유도 같다 — 실행이 한 번 밀려도
 * 다음 주기가 주워 담고, 이미 보낸 건 유니크 제약에 걸려 조용히 스킵된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    /** 일정 시각 판단은 사용자 기준(KST)으로 한다 — 서버가 UTC로 떠 있어도 흔들리지 않게. */
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final Duration TODO_LEAD = Duration.ofMinutes(30);
    private static final Duration MEETING_LEAD = Duration.ofHours(1);
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final TodoRepository todoRepository;
    private final TodoParticipantRepository todoParticipantRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    /** 5분마다 훑는다 — 리드타임(30분/1시간)보다 촘촘해야 알림이 늦지 않는다. */
    @Scheduled(cron = "${haeyaji.notification.reminder-cron:0 */5 * * * *}", zone = "Asia/Seoul")
    public void remind() {
        LocalDateTime now = LocalDateTime.now(clock.withZone(ZONE));
        try {
            remindTodos(now);
        } catch (Exception e) {
            log.error("할 일 리마인더 실패: now={}", now, e);
        }
        try {
            remindMeetings(now);
        } catch (Exception e) {
            log.error("약속 리마인더 실패: now={}", now, e);
        }
    }

    private void remindTodos(LocalDateTime now) {
        List<TodoEntity> due = findDueTodos(now, now.plus(TODO_LEAD));
        if (due.isEmpty()) {
            return;
        }
        // 공유 할 일은 수락한 참여자에게도 보낸다 — 참여자 조회는 건별이 아니라 한 번에.
        Map<UUID, List<UUID>> participantsByTodo = todoParticipantRepository
                .findByTodoIdInAndInviteStatus(
                        due.stream().map(TodoEntity::getId).toList(), InviteStatus.ACCEPTED).stream()
                .collect(Collectors.groupingBy(TodoParticipantEntity::getTodoId,
                        Collectors.mapping(TodoParticipantEntity::getMemberId, Collectors.toList())));

        for (TodoEntity todo : due) {
            String body = "%s에 시작해요.%s".formatted(
                    todo.getStartTime().format(HH_MM),
                    todo.getPlaceName() == null ? "" : " 장소: " + todo.getPlaceName());
            Set<UUID> targets = new LinkedHashSet<>();
            targets.add(todo.getMemberId());
            targets.addAll(participantsByTodo.getOrDefault(todo.getId(), List.of()));
            for (UUID memberId : targets) {
                send(memberId, NotificationCategory.TODO, NotificationType.TODO_REMINDER,
                        todo.getTitle(), body, todo.getId(), null);
            }
        }
    }

    /**
     * 자정을 넘는 창은 날짜별로 쪼갠다 — 할 일은 (날짜 + 시각)으로 나뉘어 저장돼 있어
     * 23:50에 조회하면서 다음 날 00:10 일정을 한 번에 집을 수 없다.
     */
    private List<TodoEntity> findDueTodos(LocalDateTime from, LocalDateTime to) {
        List<TodoEntity> result = new ArrayList<>(
                findDueTodosOn(from.toLocalDate(), from.toLocalTime(),
                        from.toLocalDate().equals(to.toLocalDate()) ? to.toLocalTime() : LocalTime.MAX));
        if (!from.toLocalDate().equals(to.toLocalDate())) {
            result.addAll(findDueTodosOn(to.toLocalDate(), LocalTime.MIN, to.toLocalTime()));
        }
        return result;
    }

    private List<TodoEntity> findDueTodosOn(LocalDate date, LocalTime from, LocalTime to) {
        return todoRepository.findByTodoDateAndStartTimeBetweenAndStatusAndSourceNot(
                date, from, to, TodoStatus.TODO, TodoSource.MEETING);
    }

    private void remindMeetings(LocalDateTime now) {
        List<MeetingEntity> due = meetingRepository.findByStatusAndConfirmedStartAtBetween(
                MeetingStatus.CONFIRMED, now, now.plus(MEETING_LEAD));
        for (MeetingEntity meeting : due) {
            String body = "%s에 만나기로 했어요.".formatted(meeting.getConfirmedStartAt().format(HH_MM));
            meetingParticipantRepository
                    .findByMeetingIdAndInviteStatusOrderByJoinedAt(
                            meeting.getId(), com.haeyaji.be.meeting.domain.InviteStatus.ACCEPTED)
                    .stream()
                    .map(MeetingParticipantEntity::getMemberId)
                    .forEach(memberId -> send(memberId, NotificationCategory.INVITE,
                            NotificationType.MEETING_REMINDER, meeting.getTitle(), body,
                            meeting.getId(), meeting.getShareToken()));
        }
    }

    /** 한 사람의 발송 실패가 나머지 대상을 막지 않게 한다 — 배치는 끝까지 도는 게 우선. */
    private void send(UUID memberId, NotificationCategory category, NotificationType type,
                      String title, String body, UUID refId, String linkToken) {
        try {
            notificationService.sendSystem(memberId, category, type, title, body, refId, linkToken);
        } catch (Exception e) {
            log.error("{} 알림 발송 실패: refId={}, memberId={}", type, refId, memberId, e);
        }
    }
}
