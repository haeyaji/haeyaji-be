package com.haeyaji.be.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(columnNames = {"social_type", "social_type_id"})
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_type", nullable = false, length = 20)
    private SocialType socialType;

    @Column(name = "social_type_id", nullable = false)
    private String socialTypeId;   // 소셜 타입 안에서의 고유 ID

    private String name;

    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Builder
    private User(SocialType socialType, String socialTypeId, String name, String email, UserRole role) {
        this.socialType = socialType;
        this.socialTypeId = socialTypeId;
        this.name = name;
        this.email = email;
        this.role = (role != null) ? role : UserRole.ROLE_USER;
    }

    public User update(String name, String email) {
        this.name = name;
        this.email = email;

        return this;
    }
}
