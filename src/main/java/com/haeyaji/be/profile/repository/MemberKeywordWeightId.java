package com.haeyaji.be.profile.repository;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * {@link MemberKeywordWeightEntity} 복합 PK (member_id, keyword). {@code @IdClass}용.
 */
public class MemberKeywordWeightId implements Serializable {

    private UUID memberId;
    private String keyword;

    protected MemberKeywordWeightId() {
    }

    public MemberKeywordWeightId(UUID memberId, String keyword) {
        this.memberId = memberId;
        this.keyword = keyword;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MemberKeywordWeightId that)) {
            return false;
        }
        return Objects.equals(memberId, that.memberId) && Objects.equals(keyword, that.keyword);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberId, keyword);
    }
}
