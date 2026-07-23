package com.haeyaji.be.friend.domain;

import com.haeyaji.be.common.jpa.ImmutableBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "friend")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Friend extends ImmutableBaseEntity {

    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @Column(name = "receiver_id", nullable = false)
    private UUID receiverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FriendStatus status;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Builder
    private Friend(UUID requesterId, UUID receiverId) {
        this.requesterId = requesterId;
        this.receiverId = receiverId;
        this.status = FriendStatus.PENDING;
    }

    public static Friend create(UUID requesterId, UUID receiverId) {
        return Friend.builder()
                .requesterId(requesterId)
                .receiverId(receiverId)
                .build();
    }

    public void accept() {
        this.status = FriendStatus.ACCEPTED;
        this.acceptedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = FriendStatus.REJECTED;
    }
}
