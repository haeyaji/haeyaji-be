package com.haeyaji.be.meeting.dto;

import com.haeyaji.be.meeting.domain.ResponseStatus;
import com.haeyaji.be.meeting.domain.SlotResponse;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * 칸 하나의 응답 (요청·응답 공용).
 */
public record SlotResponseItem(
        @NotNull UUID slotId,
        @NotNull ResponseStatus status
) {

    public static SlotResponseItem from(SlotResponse response) {
        return new SlotResponseItem(response.slotId(), response.status());
    }
}
