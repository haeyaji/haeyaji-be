package com.haeyaji.be.notification.repository;

import com.haeyaji.be.notification.domain.Notification;
import com.haeyaji.be.notification.domain.NotificationCategory;
import com.haeyaji.be.notification.domain.NotificationType;
import com.haeyaji.be.notification.domain.QNotification;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CustomNotificationRepositoryImpl implements CustomNotificationRepository{

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Notification> getNotifications(UUID memberId, NotificationCategory category,
                                              NotificationType type, UUID cursorId, int size) {

        BooleanBuilder booleanBuilder = new BooleanBuilder();
        QNotification notification = QNotification.notification;

        booleanBuilder.and(notification.memberId.eq(memberId));

        // 카테고리는 알림함 탭(초대·할 일·친구) 단위, 타입은 그 안의 단건 필터다. 둘 다 오면 AND로 좁힌다.
        if (category != null) {
            booleanBuilder.and(notification.category.eq(category));
        }

        if (type != null) {
            booleanBuilder.and(notification.type.eq(type));
        }

        if (cursorId != null) {
            booleanBuilder.and(notification.id.lt(cursorId));
        }

        return queryFactory.selectFrom(notification)
                .where(booleanBuilder)
                .orderBy(notification.id.desc())
                .limit(size)
                .fetch();

    }
}
