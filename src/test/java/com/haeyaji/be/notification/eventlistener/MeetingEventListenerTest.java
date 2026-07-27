package com.haeyaji.be.notification.eventlistener;

import com.haeyaji.be.meeting.domain.MeetingInviteRespondedEvent;
import com.haeyaji.be.meeting.domain.MeetingInvitedEvent;
import com.haeyaji.be.notification.domain.NotificationCategory;
import com.haeyaji.be.notification.domain.NotificationType;
import com.haeyaji.be.notification.dto.NotificationResponse;
import com.haeyaji.be.notification.redis.NotificationRedisPublisher;
import com.haeyaji.be.notification.repository.NotificationRepository;
import com.haeyaji.be.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MeetingEventListenerTest {

    @Test
    void 초대_이벤트를_받으면_발행되는_알림_내용이_이벤트값과_일치한다() {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        NotificationRedisPublisher notificationRedisPublisher = mock(NotificationRedisPublisher.class);
        NotificationService notificationService = new NotificationService(notificationRepository, notificationRedisPublisher);
        MeetingEventListener listener = new MeetingEventListener(notificationService, mock(ActorNameResolver.class));

        UUID meetingId = UUID.randomUUID();
        UUID inviterId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();
        MeetingInvitedEvent event = new MeetingInvitedEvent(
                meetingId, "shareToken123", "저녁 약속", inviterId, List.of(inviteeId));

        listener.onInvited(event);

        ArgumentCaptor<UUID> memberIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<NotificationResponse> responseCaptor = ArgumentCaptor.forClass(NotificationResponse.class);
        verify(notificationRedisPublisher).publish(memberIdCaptor.capture(), responseCaptor.capture());

        assertThat(memberIdCaptor.getValue()).isEqualTo(inviteeId);
        NotificationResponse published = responseCaptor.getValue();
        assertThat(published.type()).isEqualTo(NotificationType.MEETING_INVITE);
        assertThat(published.refId()).isEqualTo(meetingId);
        assertThat(published.linkToken()).isEqualTo("shareToken123");
    }

    @Test
    void 여러_명을_초대하면_초대받은_인원수만큼_발행이_호출된다() {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        NotificationRedisPublisher notificationRedisPublisher = mock(NotificationRedisPublisher.class);
        NotificationService notificationService = new NotificationService(notificationRepository, notificationRedisPublisher);
        MeetingEventListener listener = new MeetingEventListener(notificationService, mock(ActorNameResolver.class));

        List<UUID> invitees = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        MeetingInvitedEvent event = new MeetingInvitedEvent(
                UUID.randomUUID(), "shareToken123", "저녁 약속", UUID.randomUUID(), invitees);

        listener.onInvited(event);

        verify(notificationRedisPublisher, times(3)).publish(any(), any());
    }

    @Test
    void 초대_응답은_방장에게만_가고_수락_거절_문구가_다르다() {
        // 방장은 누가 들어왔는지 알아야 마감을 기다릴지 먼저 확정할지 판단할 수 있다.
        NotificationService notificationService = mock(NotificationService.class);
        ActorNameResolver actorNames = mock(ActorNameResolver.class);
        when(actorNames.nicknameOf(any())).thenReturn("밥");
        MeetingEventListener listener = new MeetingEventListener(notificationService, actorNames);

        UUID meetingId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID responderId = UUID.randomUUID();

        listener.onInviteResponded(new MeetingInviteRespondedEvent(
                meetingId, "tok", "저녁 약속", creatorId, responderId, true));
        listener.onInviteResponded(new MeetingInviteRespondedEvent(
                meetingId, "tok", "저녁 약속", creatorId, responderId, false));

        ArgumentCaptor<String> bodies = ArgumentCaptor.forClass(String.class);
        verify(notificationService, times(2)).send(eq(responderId), eq(creatorId),
                eq(NotificationCategory.INVITE), eq(NotificationType.MEETING_INVITE_RESPONSE),
                any(), bodies.capture(), eq(meetingId), eq("tok"));
        assertThat(bodies.getAllValues().get(0)).contains("수락");
        assertThat(bodies.getAllValues().get(1)).contains("거절");
    }

    @Test
    void 초대_응답_알림이_실패해도_예외가_밖으로_나가지_않는다() {
        NotificationService notificationService = mock(NotificationService.class);
        doThrow(new RuntimeException("db down"))
                .when(notificationService).send(any(), any(), any(), any(), any(), any(), any(), any());
        MeetingEventListener listener =
                new MeetingEventListener(notificationService, mock(ActorNameResolver.class));

        listener.onInviteResponded(new MeetingInviteRespondedEvent(
                UUID.randomUUID(), "tok", "저녁 약속", UUID.randomUUID(), UUID.randomUUID(), true));
    }
}
