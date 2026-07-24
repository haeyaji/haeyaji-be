package com.haeyaji.be.notification.repository;

import com.haeyaji.be.notification.domain.Notification;
import com.haeyaji.be.notification.domain.NotificationType;

import java.util.List;
import java.util.UUID;

public interface CustomNotificationRepository {
    List<Notification> getNotifications(UUID memberId, NotificationType type, UUID cursorId, int size);
}
