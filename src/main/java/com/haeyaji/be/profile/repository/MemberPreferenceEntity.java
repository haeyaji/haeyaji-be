package com.haeyaji.be.profile.repository;

import com.haeyaji.be.profile.domain.MemberPreference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

/**
 * 설문 preference 테이블(member_preference) 매핑.
 * <p>member와 <b>1:1 공유 PK</b> — {@code id} 값이 곧 member.id다(자동 생성 아님, 앱이 memberId로 설정).
 * 그래서 {@code UuidBaseEntity}(id 자동생성)를 상속하지 않는다.
 * <p>{@code preferredCategories}/{@code avoid}는 {@code json} 컬럼에 {@code List<String>}로 매핑한다.
 */
@Entity
@Table(name = "member_preference")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberPreferenceEntity {

    @Id
    private UUID id; // = member.id

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_categories")
    private List<String> preferredCategories;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> avoid;

    @Column(length = 20)
    private String vibe;

    @Column(length = 20)
    private String intensity;

    public static MemberPreferenceEntity create(UUID memberId) {
        MemberPreferenceEntity entity = new MemberPreferenceEntity();
        entity.id = memberId;
        return entity;
    }

    /** 부분 병합이 아니라 설문 전체를 덮어쓴다(설문은 4축을 한 번에 제출). */
    public void update(List<String> preferredCategories, List<String> avoid, String vibe, String intensity) {
        this.preferredCategories = preferredCategories;
        this.avoid = avoid;
        this.vibe = vibe;
        this.intensity = intensity;
    }

    public MemberPreference toDomain() {
        return new MemberPreference(id, preferredCategories, avoid, vibe, intensity);
    }
}
