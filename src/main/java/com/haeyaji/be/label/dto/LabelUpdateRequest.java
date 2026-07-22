package com.haeyaji.be.label.dto;

import jakarta.validation.constraints.Size;

/**
 * 라벨 수정 요청 (LABEL-3). 부분 수정(partial patch) 방식 — 필드를 안 보내면(null) 기존 값을 그대로 유지한다.
 * 길이 제한은 {@code LabelEntity} 컬럼 길이(name 30/color 20)와 맞춘다.
 */
public record LabelUpdateRequest(
        @Size(max = 30) String name,
        @Size(max = 20) String color
) {
}
