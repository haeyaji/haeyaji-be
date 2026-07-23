package com.haeyaji.be.member.domain;

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
        name = "member",
        uniqueConstraints = @UniqueConstraint(columnNames = {"social_type", "social_type_id"})
)
public class Member extends MutableBaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "social_type", nullable = false, length = 20)
    private SocialType socialType;

    @Column(name = "social_type_id", nullable = false)
    private String socialTypeId;   // 소셜 타입 안에서의 고유 ID

    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Builder
    private Member(SocialType socialType, String socialTypeId, String email, MemberRole role) {
        this.socialType = socialType;
        this.socialTypeId = socialTypeId;
        this.email = email;
        this.role = (role != null) ? role : MemberRole.ROLE_USER;
        this.status = MemberStatus.ACTIVE;
    }

    public Member update(String email) {
        this.email = email;

        return this;
    }
}
