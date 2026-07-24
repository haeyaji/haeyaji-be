package com.haeyaji.be.meeting.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * 약속 초대 요청. 초대자는 인증 principal에서 얻는다.
 * memberIds 중복은 1건으로 처리하고, 이미 참여 중인 회원은 스킵된다.
 */
public record MeetingInviteRequest(
        @NotEmpty @Size(max = 50) List<@NotNull UUID> memberIds
) {
}
