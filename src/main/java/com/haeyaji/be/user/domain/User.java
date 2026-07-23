package com.haeyaji.be.user.domain;

import com.haeyaji.be.common.jpa.MutableBaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(columnNames = {"social_type", "social_type_id"})
)
public class User extends MutableBaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "social_type", nullable = false, length = 20)
    private SocialType socialType;

    @Column(name = "social_type_id", nullable = false)
    private String socialTypeId;   // 소셜 타입 안에서의 고유 ID

    private String nickname;

    @Column(name = "friend_code", nullable = false, length = 10, unique = true)
    private String friendCode;

    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Builder
    private User(SocialType socialType, String socialTypeId, String nickname, String friendCode, String email, UserRole role) {
        this.socialType = socialType;
        this.socialTypeId = socialTypeId;
        this.nickname = nickname;
        this.friendCode = friendCode;
        this.email = email;
        this.role = (role != null) ? role : UserRole.ROLE_USER;
        this.status = UserStatus.ACTIVE;
    }

    public User update(String email) {
        this.email = email;

        return this;
    }
}
