package com.haeyaji.be.notification.repository;

import com.haeyaji.be.notification.domain.Notification;
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
    public List<Notification> getNotifications(UUID memberId, NotificationType type, UUID cursorId, int size) {

        BooleanBuilder booleanBuilder = new BooleanBuilder();
        QNotification notification = QNotification.notification;

        booleanBuilder.and(notification.memberId.eq(memberId));

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
