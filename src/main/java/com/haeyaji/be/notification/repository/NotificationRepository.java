package com.haeyaji.be.notification.repository;

import com.haeyaji.be.notification.domain.Notification;
import com.haeyaji.be.notification.domain.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID>, CustomNotificationRepository {
    long countByMemberIdAndReadFalse(UUID memberId);

    List<Notification> findByMemberIdAndReadFalse(UUID memberId);

    boolean existsByMemberIdAndTypeAndRefId(UUID memberId, NotificationType type, UUID refId);

    /** 전체 읽음 — 건별 UPDATE 대신 한 번에 처리한다. 영속성 컨텍스트는 비워 stale 엔티티를 막는다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.read = true, n.readAt = :now where n.memberId = :memberId and n.read = false")
    int markAllAsRead(@Param("memberId") UUID memberId, @Param("now") LocalDateTime now);

    /**
     * 가리키던 대상이 사라진 알림 정리 — 눌러도 404가 나는 알림을 남기지 않는다.
     * 대상 하나에 여러 수신자의 알림이 달리므로 건별 삭제 대신 한 번에 지운다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Notification n where n.refId = :refId")
    long deleteByRefId(@Param("refId") UUID refId);
}
