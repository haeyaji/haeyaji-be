package com.haeyaji.be.notification.service;

import com.haeyaji.be.common.exception.BusinessException;
import com.haeyaji.be.common.exception.ErrorCode;
import com.haeyaji.be.notification.domain.Notification;
import com.haeyaji.be.notification.domain.NotificationCategory;
import com.haeyaji.be.notification.domain.NotificationType;
import com.haeyaji.be.notification.redis.NotificationRedisPublisher;
import com.haeyaji.be.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 알림 발송·조회의 안전장치 검증 — 자기 알림 제외(NOTI-16), 멱등(NOTI-17),
 * 남의 알림 접근 차단, Redis 장애가 저장을 망치지 않는지.
 */
class NotificationServiceTest {

    private NotificationRepository repository;
    private NotificationRedisPublisher publisher;
    private NotificationService service;

    private final UUID me = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();
    private final UUID refId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(NotificationRepository.class);
        publisher = mock(NotificationRedisPublisher.class);
        service = new NotificationService(repository, publisher);
    }

    @Test
    void 자기_행동으로_생긴_알림은_자신에게_보내지_않는다() {
        Notification result = service.send(me, me, NotificationCategory.INVITE,
                NotificationType.MEETING_CONFIRMED, "제목", "본문", refId, "token");

        assertThat(result).isNull();
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void 행위자가_없어도_NPE_없이_발송된다() {
        Notification result = service.send(null, me, NotificationCategory.INVITE,
                NotificationType.MEETING_CONFIRMED, "제목", "본문", refId, "token");

        assertThat(result).isNotNull();
        verify(repository).saveAndFlush(any());
    }

    @Test
    void 스케줄_알림은_같은_대상_같은_ref로_두_번_저장되지_않는다() {
        when(repository.existsByMemberIdAndTypeAndRefId(me, NotificationType.TODO_REMINDER, refId))
                .thenReturn(true);

        Notification result = service.sendSystem(me, NotificationCategory.TODO,
                NotificationType.TODO_REMINDER, "제목", "본문", refId, null);

        assertThat(result).isNull();
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void 사전체크를_통과해도_유니크_제약에_걸리면_중복으로_보고_넘어간다() {
        when(repository.existsByMemberIdAndTypeAndRefId(any(), any(), any())).thenReturn(false);
        doThrow(new DataIntegrityViolationException("uk_noti_idem"))
                .when(repository).saveAndFlush(any());

        Notification result = service.sendSystem(me, NotificationCategory.TODO,
                NotificationType.TODO_REMINDER, "제목", "본문", refId, null);

        assertThat(result).isNull(); // 예외가 밖으로 새지 않는다
    }

    @Test
    void Redis가_죽어도_알림_저장은_유지된다() {
        doThrow(new RuntimeException("redis down")).when(publisher).publish(any(), any());

        Notification result = service.send(other, me, NotificationCategory.INVITE,
                NotificationType.MEETING_INVITE, "제목", "본문", refId, "token");

        assertThat(result).isNotNull(); // 발행 실패가 저장을 롤백시키지 않는다
        verify(repository).saveAndFlush(any());
    }

    @Test
    void 남의_알림은_읽음처리할_수_없고_존재도_알려주지_않는다() {
        UUID notificationId = UUID.randomUUID();
        Notification othersNoti = Notification.create(other, NotificationCategory.INVITE,
                NotificationType.MEETING_INVITE, "제목", "본문", refId, "token");
        when(repository.findById(notificationId)).thenReturn(Optional.of(othersNoti));

        assertThatThrownBy(() -> service.markAsRead(notificationId, me))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    void 남의_알림은_삭제할_수_없다() {
        UUID notificationId = UUID.randomUUID();
        Notification othersNoti = Notification.create(other, NotificationCategory.INVITE,
                NotificationType.MEETING_INVITE, "제목", "본문", refId, "token");
        when(repository.findById(notificationId)).thenReturn(Optional.of(othersNoti));

        assertThatThrownBy(() -> service.deleteNotification(notificationId, me))
                .isInstanceOf(BusinessException.class);
        verify(repository, never()).delete(any());
    }
}
