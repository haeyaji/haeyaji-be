package com.haeyaji.be.notification.dto;

import com.haeyaji.be.notification.domain.Notification;
import com.haeyaji.be.notification.domain.NotificationCategory;
import com.haeyaji.be.notification.domain.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationCategory category,
        NotificationType type,
        String title,
        String body,
        UUID refId,
        boolean read,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getCategory(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getRefId(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}
