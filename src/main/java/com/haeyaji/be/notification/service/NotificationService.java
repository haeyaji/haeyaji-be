package com.haeyaji.be.notification.service;

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

    /**
     * 알림 목록 조회 (커서 페이지네이션 + type 필터). NOTI-1
     * TODO
     * 1) type이 null이 아니면 memberId+type 조건, null이면 memberId만으로 조회
     * 2) cursorId가 있으면 그 id보다 이전(더 오래된) 알림만 — id가 UUIDv7이라 생성 순서와 id 순서가 같음
     * 3) size만큼 limit, 최신순(id 내림차순) 정렬
     * 4) 필터·정렬 조합이 늘어나면 CustomNotificationRepository(QueryDSL)로 분리 고려 (friend 패키지 참고)
     */
    public List<Notification> getNotifications(UUID memberId, NotificationType type, UUID cursorId, int size) {
        throw new UnsupportedOperationException("TODO: 커서 페이지네이션 알림 목록 조회");
    }

    /**
     * 읆지 않은 알림 개수. NOTI-2
     * TODO
     * 1) notificationRepository에 countByMemberIdAndReadFalse(UUID memberId) 파생 쿼리 추가해서 사용
     */
    public long getUnreadCount(UUID memberId) {
        throw new UnsupportedOperationException("TODO: 미읽음 개수 조회");
    }

    /**
     * 알림 단건 읽음 처리. NOTI-3
     * TODO
     * 1) notificationRepository.findById(notificationId)로 조회, 없으면 NOTIFICATION_NOT_FOUND
     * 2) 조회한 알림의 memberId != 파라미터 memberId면 NOTIFICATION_FORBIDDEN (남의 알림 못 건드림)
     * 3) notification.markAsRead() 호출 — 영속 상태 엔티티라 더티체킹으로 flush됨, 별도 save 불필요
     */
    @Transactional
    public void markAsRead(UUID notificationId, UUID memberId) {
        throw new UnsupportedOperationException("TODO: 알림 단건 읽음 처리");
    }

    /**
     * 알림 전체 읽음 처리. NOTI-3
     * TODO
     * 1) notificationRepository에 findByMemberIdAndReadFalse(UUID memberId) 파생 쿼리 추가
     * 2) 조회된 리스트를 순회하며 각각 markAsRead() — 건수가 많아지면 벌크 update 쿼리로 바꿀지 고려
     */
    @Transactional
    public void markAllAsRead(UUID memberId) {
        throw new UnsupportedOperationException("TODO: 알림 전체 읽음 처리");
    }

    /**
     * 알림 삭제. NOTI-4
     * TODO
     * 1) notificationRepository.findById(notificationId)로 조회, 없으면 NOTIFICATION_NOT_FOUND
     * 2) 조회한 알림의 memberId != 파라미터 memberId면 NOTIFICATION_FORBIDDEN
     * 3) notificationRepository.delete(notification)
     */
    @Transactional
    public void deleteNotification(UUID notificationId, UUID memberId) {
        throw new UnsupportedOperationException("TODO: 알림 삭제");
    }

    /**
     * 알림 발송 — 다른 도메인 서비스(FriendService, MeetingService 등)가 호출하는 공통 진입점. NOTI-6~15
     * TODO
     * 1) (NOTI-17 멱등성) notificationRepository에 existsByMemberIdAndTypeAndRefId(memberId, type, refId) 추가해서
     *    사전 체크 — 이미 있으면 그냥 return (재발송 안 함)
     * 2) Notification.create(memberId, category, type, title, body, refId)로 생성
     * 3) notificationRepository.saveAndFlush(notification)로 즉시 INSERT (동시 발송 레이스 대비)
     *    — (member_id, type, ref_id) 유니크 제약이 DDL에 추가되면 DataIntegrityViolationException을 여기서 잡아서 무시
     * 4) (NOTI-16 본인 제외) 이 메서드는 memberId가 누구인지만 알 뿐 "행위자가 누구인지" 모름 —
     *    호출부(FriendService.sendRequest 등)가 "행위자 != 수신자"를 먼저 확인하고 호출해야 함. 여기서 강제 못 함.
     */
    @Transactional
    public Notification send(UUID memberId, NotificationCategory category, NotificationType type,
                              String title, String body, UUID refId) {
        throw new UnsupportedOperationException("TODO: 알림 발송 (멱등성 체크 포함)");
    }
}
