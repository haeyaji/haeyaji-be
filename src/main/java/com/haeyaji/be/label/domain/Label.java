package com.haeyaji.be.label.domain;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 라벨 도메인 모델 (LABEL). DB 매핑({@code repository.LabelEntity})과 분리된 순수 객체.
 */
@Builder
public record Label(
        UUID id,
        UUID memberId,
        String name,
        String color,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
