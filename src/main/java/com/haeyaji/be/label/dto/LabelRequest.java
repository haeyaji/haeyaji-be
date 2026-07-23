package com.haeyaji.be.label.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 라벨 등록 요청 (LABEL-1). name은 필수, color는 선택.
 * 길이 제한은 {@code LabelEntity} 컬럼 길이(name 30/color 20)와 맞춘다.
 * memberId는 인증 미구현으로 아직 요청에 안 받음(추후 로그인 붙을 때 추가).
 */
public record LabelRequest(
        @NotBlank @Size(max = 30) String name,
        @Size(max = 20) String color
) {
}
