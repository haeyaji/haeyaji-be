package com.haeyaji.be.friend.dto;

import com.haeyaji.be.friend.domain.Friend;
import com.haeyaji.be.friend.domain.FriendStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * TODO: 지금은 Friend 엔티티 필드를 그대로 노출만 함.
 * 실제로는 상대방의 nickname/friendCode 등도 같이 내려줘야 프론트에서 바로 쓸 수 있음 —
 * MemberRepository 조회 결과를 합쳐서 만들지, 여기 필드를 늘릴지 정해서 채우기.
 */
public record FriendResponse(
        UUID id,
        UUID requesterId,
        UUID receiverId,
        FriendStatus status,
        LocalDateTime createdAt,
        LocalDateTime acceptedAt
) {

    public static FriendResponse from(Friend friend) {
        return new FriendResponse(
                friend.getId(),
                friend.getRequesterId(),
                friend.getReceiverId(),
                friend.getStatus(),
                friend.getCreatedAt(),
                friend.getAcceptedAt()
        );
    }
}
