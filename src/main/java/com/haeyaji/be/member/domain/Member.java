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

    // OAuth 첫 로그인 시점엔 아직 없음(온보딩에서 별도 API로 채움) → nullable 허용.
    // unique=true여도 MySQL은 NULL끼리는 유니크 충돌로 안 봐서 온보딩 전 회원이 여럿이어도 안전함.
    @Column(unique = true, length = 20)
    private String nickname;

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

    public Member assignNickname(String nickname) {
        this.nickname = nickname;

        return this;
    }
}
