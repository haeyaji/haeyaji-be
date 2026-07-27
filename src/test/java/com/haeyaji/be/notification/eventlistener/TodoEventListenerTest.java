package com.haeyaji.be.notification.eventlistener;

import com.haeyaji.be.notification.domain.NotificationCategory;
import com.haeyaji.be.notification.domain.NotificationType;
import com.haeyaji.be.notification.service.NotificationService;
import com.haeyaji.be.todo.domain.SharedTodoUpdatedEvent;
import com.haeyaji.be.todo.domain.TodoShareRespondedEvent;
import com.haeyaji.be.todo.domain.TodoSharedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 할 일 공유 알림 — 초대·응답·수정이 각각 누구에게 가는지.
 */
class TodoEventListenerTest {

    private NotificationService notificationService;
    private TodoEventListener listener;

    private final UUID todoId = UUID.randomUUID();
    private final UUID owner = UUID.randomUUID();
    private final UUID invitee1 = UUID.randomUUID();
    private final UUID invitee2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        ActorNameResolver actorNames = mock(ActorNameResolver.class);
        when(actorNames.nicknameOf(any())).thenReturn("앨리스");
        listener = new TodoEventListener(notificationService, actorNames);
    }

    @Test
    void 초대는_초대받은_사람_수만큼_발송된다() {
        listener.onShared(new TodoSharedEvent(todoId, "장보기", owner, List.of(invitee1, invitee2)));

        verify(notificationService).send(eq(owner), eq(invitee1), eq(NotificationCategory.INVITE),
                eq(NotificationType.SHARE_INVITE), any(), any(), eq(todoId), eq(null));
        verify(notificationService).send(eq(owner), eq(invitee2), any(),
                eq(NotificationType.SHARE_INVITE), any(), any(), eq(todoId), eq(null));
    }

    @Test
    void 초대_응답은_주인에게만_간다() {
        listener.onShareResponded(new TodoShareRespondedEvent(todoId, "장보기", owner, invitee1, true));

        verify(notificationService, times(1)).send(eq(invitee1), eq(owner), any(),
                eq(NotificationType.SHARE_INVITE_RESPONSE), any(), any(), eq(todoId), eq(null));
    }

    @Test
    void 공유_일정_수정은_관련자_전원에게_보내고_제외는_발송단이_판단한다() {
        // 수정한 본인 제외는 NotificationService.send가 하므로(NOTI-16), 리스너는 대상 전원을 그대로 넘긴다.
        listener.onSharedTodoUpdated(
                new SharedTodoUpdatedEvent(todoId, "장보기", invitee1, List.of(invitee1, invitee2, owner)));

        verify(notificationService, times(3)).send(eq(invitee1), any(), eq(NotificationCategory.TODO),
                eq(NotificationType.TODO_SHARED_UPDATED), any(), any(), eq(todoId), eq(null));
    }

    @Test
    void 수정_알림_문구에_할_일_제목이_들어간다() {
        listener.onSharedTodoUpdated(
                new SharedTodoUpdatedEvent(todoId, "장보기", invitee1, List.of(owner)));

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(notificationService).send(any(), any(), any(), any(), any(), body.capture(), any(), any());
        assertThat(body.getValue()).contains("장보기");
    }
}
