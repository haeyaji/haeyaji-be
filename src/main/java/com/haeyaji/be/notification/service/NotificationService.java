package com.haeyaji.be.notification.service;

import com.haeyaji.be.common.exception.BusinessException;
import com.haeyaji.be.common.exception.ErrorCode;
import com.haeyaji.be.common.response.ApiResponse;
import com.haeyaji.be.common.response.CursorPageResponse;
import com.haeyaji.be.notification.domain.Notification;
import com.haeyaji.be.notification.domain.NotificationCategory;
import com.haeyaji.be.notification.domain.NotificationType;
import com.haeyaji.be.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public CursorPageResponse<Notification, UUID> getNotifications(UUID memberId, NotificationType type, UUID cursorId, int size) {

        List<Notification> notiList = notificationRepository.getNotifications(memberId, type, cursorId, size + 1);

        boolean hasNext = false;
        UUID nextCursor = null;

        // hasNext, nextCursor 판단을 위해 size + 1만큼 조회
        if (notiList.size() == size + 1) {
            nextCursor = notiList.get(size).getId();
            notiList.remove(size);
            hasNext = true;
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

        if (noti.getMemberId() != memberId) {
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

        if (noti.getMemberId() != memberId) {
            throw new BusinessException(ErrorCode.NOTIFICATION_FORBIDDEN);
        }

        notificationRepository.delete(noti);
    }

     // 행위자 != 수신자는 호출부에서 필터링
    @Transactional
    public Notification send(UUID memberId, NotificationCategory category, NotificationType type,
                              String title, String body, UUID refId) {

        // 이미 같은 알림이 발송되었을 경우 return (멱등)
        if (notificationRepository.existsByMemberIdAndTypeAndRefId(memberId, type, refId)) {
            return null;
        }

        Notification noti = Notification.create(memberId, category, type, title, body, refId);

        // DataIntegrityViolationException 고려
        notificationRepository.save(noti);

        return noti;
    }
}
