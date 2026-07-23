package com.haeyaji.be.label.dto;

import com.haeyaji.be.label.domain.Label;

import java.util.UUID;

/**
 * 라벨 응답 (camelCase).
 */
public record LabelResponse(
        UUID id,
        String name,
        String color
) {

    public static LabelResponse from(Label label) {
        return new LabelResponse(label.id(), label.name(), label.color());
    }
}
