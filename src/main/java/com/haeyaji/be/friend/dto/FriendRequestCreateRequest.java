package com.haeyaji.be.friend.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FriendRequestCreateRequest(
        @NotNull UUID receiverId
) {
}
