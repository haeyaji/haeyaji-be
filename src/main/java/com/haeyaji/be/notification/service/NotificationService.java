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
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationRedisPublisher notificationRedisPublisher;

    // Todo: 같은 refId로 여러 번 알림 발송하는 것이 정당한 경우는 멱등성 체크 제외
    private static final Set<NotificationType> IDEMPOTENT_TYPES =
            Set.of(NotificationType.TODO_REMINDER, NotificationType.MEETING_REMINDER, NotificationType.TODO_WEATHER_ALERT);

    public CursorPageResponse<Notification, UUID> getNotifications(
            UUID memberId, NotificationCategory category, NotificationType type, UUID cursorId, int size) {

        // hasNext, nextCursor 판단을 위해 size + 1만큼 조회
        List<Notification> notiList =
                notificationRepository.getNotifications(memberId, category, type, cursorId, size + 1);

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
            // 남의 알림 id인지조차 알려주지 않는다(존재 여부 유출 방지) — 없는 것과 같게 응답.
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }

        noti.markAsRead();
    }

    /** 미읽음이 수천 건이어도 UPDATE 한 번으로 끝낸다(건별 UPDATE는 요청 하나가 DB를 오래 잡는다). */
    @Transactional
    public void markAllAsRead(UUID memberId) {
        notificationRepository.markAllAsRead(memberId, LocalDateTime.now());
    }

    @Transactional
    public void deleteNotification(UUID notificationId, UUID memberId) {
        Notification noti = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!noti.getMemberId().equals(memberId)) {
            // 남의 알림 id인지조차 알려주지 않는다(존재 여부 유출 방지) — 없는 것과 같게 응답.
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }

        notificationRepository.delete(noti);
    }

    /**
     *      actor가 있는 알림은 send, 없으면 sendSystem (actorId = null)
     */

    // AFTER_COMMIT 이벤트 리스너에서 호출된다 — 이미 커밋된 트랜잭션에 REQUIRED로 참여하면
    // 저장이 커밋되지 않고 조용히 사라진다. 그래서 새 트랜잭션에서 저장한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification send(UUID actorId, UUID memberId, NotificationCategory category, NotificationType type,

                             String title, String body, UUID refId, String linkToken) {
        if (Objects.equals(actorId, memberId)) {
            return null; // 자기 행동으로 생긴 알림은 자신에게 보내지 않는다(NOTI-16)
        }

        return doSend(memberId, category, type, title, body, refId, linkToken);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification sendSystem(UUID memberId, NotificationCategory category, NotificationType type,
                                   String title, String body, UUID refId, String linkToken) {

        return doSend(memberId, category, type, title, body, refId, linkToken);
    }

    private Notification doSend(UUID memberId, NotificationCategory category, NotificationType type,
                                String title, String body, UUID refId, String linkToken) {

        // refId가 null이면 'ref_id = null' 비교가 항상 거짓이라 중복을 못 거른다 → 대상에서 제외.
        if (IDEMPOTENT_TYPES.contains(type) && refId != null
                && notificationRepository.existsByMemberIdAndTypeAndRefId(memberId, type, refId)) {
            return null;
        }

        Notification noti = Notification.create(memberId, category, type, title, body, refId, linkToken);
        try {
            notificationRepository.saveAndFlush(noti);
        } catch (DataIntegrityViolationException e) {
            // 사전 체크와 저장 사이 경합(스케줄러 중복 실행 등) — 유니크 제약이 최종 방어선(NOTI-17).
            log.debug("이미 발송된 알림이라 건너뜀: member={} type={} ref={}", memberId, type, refId);
            return null;
        }

        // 실시간 push는 부가 기능 — Redis가 죽어도 알림 저장까지 롤백되면 안 된다(로그만 남기고 계속).
        try {
            notificationRedisPublisher.publish(memberId, NotificationResponse.from(noti));
        } catch (Exception e) {
            log.warn("알림 실시간 발행 실패(저장은 완료): member={} notiId={} err={}", memberId, noti.getId(), e.toString());
        }

        return noti;
    }
}
