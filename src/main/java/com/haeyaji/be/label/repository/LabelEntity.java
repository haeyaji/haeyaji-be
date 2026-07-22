package com.haeyaji.be.label.repository;

import com.haeyaji.be.common.jpa.MutableBaseEntity;
import com.haeyaji.be.label.domain.Label;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 라벨 테이블(label) 매핑. 비즈니스 로직은 여기 두지 않고 {@code domain.Label}로 변환해 넘긴다.
 * id/createdAt/updatedAt은 {@link MutableBaseEntity}에서 상속.
 * member_id는 ERD상 NN이지만, user 도메인(인증) 미구현으로 당분간 nullable로 둔다 —
 * TodoEntity.sourceRefId와 같은 느슨한 UUID 컬럼 방식(로그인 붙을 때 값 채움).
 */
@Entity
@Table(name = "label")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LabelEntity extends MutableBaseEntity {

    @Column(name = "member_id")
    private UUID memberId;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(length = 20)
    private String color;

    public static LabelEntity create(UUID memberId, String name, String color) {
        LabelEntity entity = new LabelEntity();
        entity.memberId = memberId;
        entity.name = name;
        entity.color = color;
        return entity;
    }

    public Label toDomain() {
        return Label.builder()
                .id(getId())
                .memberId(memberId)
                .name(name)
                .color(color)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}
