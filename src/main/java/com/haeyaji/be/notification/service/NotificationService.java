package com.haeyaji.be.notification.service;

import com.haeyaji.be.common.exception.BusinessException;
import com.haeyaji.be.common.exception.ErrorCode;
import com.haeyaji.be.common.response.ApiResponse;
import com.haeyaji.be.common.response.CursorPageResponse;
import com.haeyaji.be.notification.domain.Notification;
import com.haeyaji.be.notification.domain.NotificationCategory;
import com.haeyaji.be.notification.domain.NotificationType;
import com.haeyaji.be.notification.dto.NotificationResponse;
import com.haeyaji.be.notification.redis.NotificationRedisPublisher;
import com.haeyaji.be.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationRedisPublisher notificationRedisPublisher;

    // Todo: 같은 refId로 여러 번 알림 발송하는 것이 정당한 경우는 멱등성 체크 제외
    private static final Set<NotificationType> IDEMPOTENT_TYPES =
            Set.of(NotificationType.TODO_REMINDER, NotificationType.MEETING_REMINDER, NotificationType.TODO_WEATHER_ALERT);

    public CursorPageResponse<Notification, UUID> getNotifications(UUID memberId, NotificationType type, UUID cursorId, int size) {

        // hasNext, nextCursor 판단을 위해 size + 1만큼 조회
        List<Notification> notiList = notificationRepository.getNotifications(memberId, type, cursorId, size + 1);

        boolean hasNext = false;
        UUID nextCursor = null;

        if (notiList.size() == size + 1) {
            notiList.remove(size);
            hasNext = true;
        }

        if (!notiList.isEmpty()) {
            nextCursor = notiList.getLast().getId();
        }

        return CursorPageResponse.of(notiList, nextCursor, hasNext);
    }

    public long getUnreadCount(UUID memberId) {
        return notificationRepository.countByMemberIdAndReadFalse(memberId);
    }

    @Transactional
    public void markAsRead(UUID notificationId, UUID memberId) {
        Notification noti = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!noti.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_FORBIDDEN);
        }

        noti.markAsRead();
    }

    // 벌크 update 쿼리도 고려 가능
    @Transactional
    public void markAllAsRead(UUID memberId) {
        List<Notification> notiList = notificationRepository.findByMemberIdAndReadFalse(memberId);

        for (Notification noti : notiList) {
            noti.markAsRead();
        }
    }

    @Transactional
    public void deleteNotification(UUID notificationId, UUID memberId) {
        Notification noti = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!noti.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_FORBIDDEN);
        }

        notificationRepository.delete(noti);
    }

    /**
     *      actor가 있는 알림은 send, 없으면 sendSystem (actorId = null)
     */

    @Transactional
    public Notification send(UUID actorId, UUID memberId, NotificationCategory category, NotificationType type,

                             String title, String body, UUID refId, String linkToken) {
        if (actorId.equals(memberId)) {
            return null;
        }

        return doSend(memberId, category, type, title, body, refId, linkToken);
    }

    @Transactional
    public Notification sendSystem(UUID memberId, NotificationCategory category, NotificationType type,
                                   String title, String body, UUID refId, String linkToken) {

        return doSend(memberId, category, type, title, body, refId, linkToken);
    }

    private Notification doSend(UUID memberId, NotificationCategory category, NotificationType type,
                                String title, String body, UUID refId, String linkToken) {

        if (IDEMPOTENT_TYPES.contains(type)
                && notificationRepository.existsByMemberIdAndTypeAndRefId(memberId, type, refId)) {
            return null;
        }

        Notification noti = Notification.create(memberId, category, type, title, body, refId, linkToken);
        notificationRepository.save(noti);

        // 저장 성공 후 실시간 push용 Redis 발행 — memberId별 채널이라 그 채널을 구독 중인 인스턴스만 받음
        notificationRedisPublisher.publish(memberId, NotificationResponse.from(noti));

        return noti;
    }
}
