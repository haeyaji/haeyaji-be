package com.haeyaji.be.meeting.domain;

import java.util.UUID;

/** 칸 하나에 대한 한 사람의 FREE/BUSY 응답. 행이 없으면 미응답. */
public record SlotResponse(
        UUID id,
        UUID slotId,
        UUID memberId,
        ResponseStatus status
) {
}
