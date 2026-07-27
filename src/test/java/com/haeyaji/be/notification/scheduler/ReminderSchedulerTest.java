package com.haeyaji.be.notification.scheduler;

import com.haeyaji.be.meeting.domain.MeetingStatus;
import com.haeyaji.be.meeting.repository.MeetingParticipantRepository;
import com.haeyaji.be.meeting.repository.MeetingRepository;
import com.haeyaji.be.notification.domain.Notification;
import com.haeyaji.be.notification.domain.NotificationType;
import com.haeyaji.be.notification.mail.ReminderMailer;
import com.haeyaji.be.notification.service.NotificationService;
import com.haeyaji.be.todo.domain.InviteStatus;
import com.haeyaji.be.todo.domain.TodoSource;
import com.haeyaji.be.todo.domain.TodoStatus;
import com.haeyaji.be.todo.repository.TodoEntity;
import com.haeyaji.be.todo.repository.TodoParticipantEntity;
import com.haeyaji.be.todo.repository.TodoParticipantRepository;
import com.haeyaji.be.todo.repository.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 다가온 일정 리마인더 — 조회 창을 어떻게 자르는지, 누구에게 보내는지.
 * <p>중복 방지는 알림 저장 계층(유니크 제약)의 몫이라 여기선 검증하지 않는다.
 */
class ReminderSchedulerTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private TodoRepository todoRepository;
    private TodoParticipantRepository todoParticipantRepository;
    private MeetingRepository meetingRepository;
    private MeetingParticipantRepository meetingParticipantRepository;
    private NotificationService notificationService;
    private ReminderMailer reminderMailer;

    @BeforeEach
    void setUp() {
        todoRepository = mock(TodoRepository.class);
        todoParticipantRepository = mock(TodoParticipantRepository.class);
        meetingRepository = mock(MeetingRepository.class);
        meetingParticipantRepository = mock(MeetingParticipantRepository.class);
        notificationService = mock(NotificationService.class);
        reminderMailer = mock(ReminderMailer.class);
        when(todoParticipantRepository.findByTodoIdInAndInviteStatus(any(), any())).thenReturn(List.of());
        when(meetingRepository.findByStatusAndConfirmedStartAtBetween(any(), any(), any())).thenReturn(List.of());
    }

    private ReminderScheduler schedulerAt(String kstInstant) {
        Clock clock = Clock.fixed(Instant.parse(kstInstant), KST);
        return new ReminderScheduler(todoRepository, todoParticipantRepository, meetingRepository,
                meetingParticipantRepository, notificationService, reminderMailer, clock);
    }

    @Test
    void 하루_안에_끝나는_창은_그날_한_번만_조회한다() {
        when(todoRepository.findByTodoDateAndStartTimeBetweenAndStatusAndSourceNot(
                any(), any(), any(), any(), any())).thenReturn(List.of());

        schedulerAt("2026-07-27T05:00:00Z").remind(); // KST 14:00

        verify(todoRepository).findByTodoDateAndStartTimeBetweenAndStatusAndSourceNot(
                eq(LocalDate.of(2026, 7, 27)), eq(LocalTime.of(14, 0)), eq(LocalTime.of(14, 10)),
                eq(TodoStatus.TODO), eq(TodoSource.MEETING));
    }

    @Test
    void 자정을_넘는_창은_두_날짜로_나눠_조회한다() {
        // 할 일은 (날짜 + 시각)으로 나뉘어 있어 23:50~00:20을 한 번에 집을 수 없다.
        when(todoRepository.findByTodoDateAndStartTimeBetweenAndStatusAndSourceNot(
                any(), any(), any(), any(), any())).thenReturn(List.of());

        schedulerAt("2026-07-27T14:55:00Z").remind(); // KST 23:55

        verify(todoRepository).findByTodoDateAndStartTimeBetweenAndStatusAndSourceNot(
                eq(LocalDate.of(2026, 7, 27)), eq(LocalTime.of(23, 55)), eq(LocalTime.MAX), any(), any());
        verify(todoRepository).findByTodoDateAndStartTimeBetweenAndStatusAndSourceNot(
                eq(LocalDate.of(2026, 7, 28)), eq(LocalTime.MIN), eq(LocalTime.of(0, 5)), any(), any());
    }

    @Test
    void 공유_할_일은_수락한_참여자에게도_간다() {
        UUID owner = UUID.randomUUID();
        UUID participant = UUID.randomUUID();
        UUID todoId = UUID.randomUUID();
        // 저장 전 엔티티는 id가 비어 있어 참여자와 이어붙일 수 없으므로, 저장된 상태를 흉내 낸다.
        TodoEntity todo = mock(TodoEntity.class);
        when(todo.getId()).thenReturn(todoId);
        when(todo.getMemberId()).thenReturn(owner);
        when(todo.getTitle()).thenReturn("회의");
        when(todo.getStartTime()).thenReturn(LocalTime.of(14, 5));
        when(todoRepository.findByTodoDateAndStartTimeBetweenAndStatusAndSourceNot(
                any(), any(), any(), any(), any())).thenReturn(List.of(todo));

        TodoParticipantEntity accepted = mock(TodoParticipantEntity.class);
        when(accepted.getTodoId()).thenReturn(todoId);
        when(accepted.getMemberId()).thenReturn(participant);
        when(todoParticipantRepository.findByTodoIdInAndInviteStatus(any(), eq(InviteStatus.ACCEPTED)))
                .thenReturn(List.of(accepted));

        schedulerAt("2026-07-27T05:00:00Z").remind();

        verify(notificationService).sendSystem(eq(owner), any(),
                eq(NotificationType.TODO_REMINDER), any(), any(), eq(todoId), eq(null));
        verify(notificationService).sendSystem(eq(participant), any(),
                eq(NotificationType.TODO_REMINDER), any(), any(), eq(todoId), eq(null));
    }

    @Test
    void 이미_보낸_알림이면_메일도_다시_나가지_않는다() {
        // 스케줄러는 5분마다 같은 일정을 다시 집는다. 중복 알림은 유니크 제약이 거르지만 메일은 안 걸러지므로,
        // 알림이 실제로 만들어졌을 때만 보내야 한다 — 아니면 5분마다 같은 메일이 나간다.
        UUID todoId = UUID.randomUUID();
        TodoEntity todo = mock(TodoEntity.class);
        when(todo.getId()).thenReturn(todoId);
        when(todo.getMemberId()).thenReturn(UUID.randomUUID());
        when(todo.getTitle()).thenReturn("회의");
        when(todo.getStartTime()).thenReturn(LocalTime.of(14, 5));
        when(todoRepository.findByTodoDateAndStartTimeBetweenAndStatusAndSourceNot(
                any(), any(), any(), any(), any())).thenReturn(List.of(todo));
        when(notificationService.sendSystem(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(null); // 이미 보낸 건이라 걸러짐

        schedulerAt("2026-07-27T05:00:00Z").remind();

        verify(reminderMailer, never()).send(any(), any(), any(), any(), any());
    }

    @Test
    void 알림이_새로_만들어지면_메일이_나간다() {
        UUID owner = UUID.randomUUID();
        TodoEntity todo = mock(TodoEntity.class);
        when(todo.getId()).thenReturn(UUID.randomUUID());
        when(todo.getMemberId()).thenReturn(owner);
        when(todo.getTitle()).thenReturn("한강 산책");
        when(todo.getStartTime()).thenReturn(LocalTime.of(14, 5));
        when(todo.getPlaceName()).thenReturn("한강공원");
        when(todoRepository.findByTodoDateAndStartTimeBetweenAndStatusAndSourceNot(
                any(), any(), any(), any(), any())).thenReturn(List.of(todo));
        when(notificationService.sendSystem(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(mock(Notification.class));

        schedulerAt("2026-07-27T05:00:00Z").remind();

        // 날씨 문구는 이 배치가 알 수 없다 — 궂은 날씨는 WeatherAlertScheduler가 따로 알린다.
        verify(reminderMailer).send(eq(owner), eq("한강 산책"), eq(LocalTime.of(14, 5)), eq("한강공원"), eq(null));
    }

    @Test
    void 약속에서_생긴_할_일은_할_일_리마인더에서_빠진다() {
        // 같은 약속을 MEETING_REMINDER가 이미 알리므로 두 번 울리면 안 된다.
        schedulerAt("2026-07-27T05:00:00Z").remind();

        verify(todoRepository, never()).findByTodoDateAndStartTimeBetweenAndStatusAndSourceNot(
                any(), any(), any(), any(), eq(TodoSource.MANUAL));
        verify(todoRepository).findByTodoDateAndStartTimeBetweenAndStatusAndSourceNot(
                any(), any(), any(), any(), eq(TodoSource.MEETING));
    }

    @Test
    void 확정된_약속만_리마인더_대상이다() {
        schedulerAt("2026-07-27T05:00:00Z").remind();

        verify(meetingRepository).findByStatusAndConfirmedStartAtBetween(
                eq(MeetingStatus.CONFIRMED), any(), any());
    }

    @Test
    void 할_일_조회가_실패해도_약속_리마인더는_돈다() {
        // 배치는 한쪽이 죽어도 끝까지 도는 게 우선이다.
        when(todoRepository.findByTodoDateAndStartTimeBetweenAndStatusAndSourceNot(
                any(), any(), any(), any(), any())).thenThrow(new RuntimeException("db down"));

        schedulerAt("2026-07-27T05:00:00Z").remind();

        verify(meetingRepository).findByStatusAndConfirmedStartAtBetween(any(), any(), any());
    }
}
