package com.haeyaji.be.notification.eventlistener;

import com.haeyaji.be.friend.domain.FriendRequestedEvent;
import com.haeyaji.be.friend.domain.FriendRespondedEvent;
import com.haeyaji.be.notification.domain.NotificationCategory;
import com.haeyaji.be.notification.domain.NotificationType;
import com.haeyaji.be.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 친구 요청·응답 알림 — 누구에게 가는지와 문구가 맞는지, 발송 실패가 전파되지 않는지.
 */
class FriendEventListenerTest {

    private NotificationService notificationService;
    private FriendEventListener listener;

    private final UUID friendId = UUID.randomUUID();
    private final UUID requester = UUID.randomUUID();
    private final UUID receiver = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        ActorNameResolver actorNames = mock(ActorNameResolver.class);
        when(actorNames.nicknameOf(requester)).thenReturn("앨리스");
        when(actorNames.nicknameOf(receiver)).thenReturn("밥");
        listener = new FriendEventListener(notificationService, actorNames);
    }

    @Test
    void 친구_요청은_받는_사람에게_요청자_이름과_함께_간다() {
        listener.onRequested(new FriendRequestedEvent(friendId, requester, receiver));

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(notificationService).send(eq(requester), eq(receiver),
                eq(NotificationCategory.FRIEND), eq(NotificationType.FRIEND_REQUEST),
                any(), body.capture(), eq(friendId), eq(null));
        assertThat(body.getValue()).contains("앨리스");
    }

    @Test
    void 수락_응답은_요청을_보냈던_쪽에_간다() {
        listener.onResponded(new FriendRespondedEvent(friendId, receiver, requester, true));

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        // actor는 응답한 사람(receiver), 수신자는 요청을 보냈던 사람(requester).
        verify(notificationService).send(eq(receiver), eq(requester),
                eq(NotificationCategory.FRIEND), eq(NotificationType.FRIEND_RESPONSE),
                any(), body.capture(), eq(friendId), eq(null));
        assertThat(body.getValue()).contains("밥").contains("친구가 됐어요");
    }

    @Test
    void 거절도_알린다_다시_보낼지_판단할_수_있어야_하므로() {
        listener.onResponded(new FriendRespondedEvent(friendId, receiver, requester, false));

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(notificationService).send(any(), eq(requester), any(),
                eq(NotificationType.FRIEND_RESPONSE), any(), body.capture(), any(), any());
        assertThat(body.getValue()).contains("거절");
    }

    @Test
    void 알림_발송이_실패해도_예외가_밖으로_나가지_않는다() {
        // 커밋 이후 리스너라 여기서 터지면 원래 요청은 이미 성공한 뒤다 — 삼키고 로그만 남겨야 한다.
        doThrow(new RuntimeException("redis down"))
                .when(notificationService).send(any(), any(), any(), any(), any(), any(), any(), any());

        listener.onRequested(new FriendRequestedEvent(friendId, requester, receiver));
    }
}
