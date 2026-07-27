package com.haeyaji.be.notification.domain;

import com.haeyaji.be.common.jpa.ImmutableBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
// 알림함 조회는 늘 "내 것 + 최신순"이라 (member_id, id) 인덱스가 필수 — 없으면 매 조회가 풀스캔이다.
// uk_noti_idem: 스케줄 알림(리마인더 등) 중복 발송의 최종 방어선(NOTI-17).
//   ref_id가 NULL인 즉발성 알림은 MySQL이 NULL을 유일값으로 봐서 제약 대상이 아니다.
@Table(name = "notification",
        indexes = {
                @Index(name = "idx_noti_inbox", columnList = "member_id, id"),
                @Index(name = "idx_noti_unread", columnList = "member_id, is_read")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_noti_idem", columnNames = {"member_id", "type", "ref_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends ImmutableBaseEntity {

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 255)
    private String body;

    // 연관 대상 id (meeting/todoo 등)
    @Column(name = "ref_id")
    private UUID refId;

    // 딥링크용 토큰 (예: meeting.shareToken). 대상 리소스가 shareToken 기반으로만 조회되는 경우 사용.
    // shareToken은 SecureRandom 256bit의 URL-safe Base64라 43자 → 20자로는 저장이 잘려 실패한다.
    @Column(name = "link_token", length = 64)
    private String linkToken;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Notification(UUID memberId, NotificationCategory category, NotificationType type, String title, String body, UUID refId, String linkToken) {
        this.memberId = memberId;
        this.category = category;
        this.type = type;
        this.title = title;
        this.body = body;
        this.refId = refId;
        this.linkToken = linkToken;
        this.read = false;
        this.readAt = null;
    }

    public static Notification create(UUID memberId, NotificationCategory category, NotificationType type, String title, String body, UUID refId, String linkToken) {
        return Notification.builder()
                .memberId(memberId)
                .category(category)
                .type(type)
                .title(title)
                .body(body)
                .refId(refId)
                .linkToken(linkToken)
                .build();
    }

    public void markAsRead() {
        if (!this.read) {
            this.readAt = LocalDateTime.now();
        }

        this.read = true;

    }
}
