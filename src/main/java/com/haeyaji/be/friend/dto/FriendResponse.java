package com.haeyaji.be.friend.dto;

import com.haeyaji.be.friend.domain.Friend;
import com.haeyaji.be.friend.domain.FriendStatus;
import com.haeyaji.be.friend.domain.FriendWithCounterpart;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 친구 관계 1건. <b>상대방 id·닉네임</b>을 함께 내려준다 — 관계 행에는 양쪽 id만 있어서,
 * 이것만으로는 화면에 이름을 띄울 수 없었다(회원을 따로 조회하거나 검색으로 캐시해 둔 이름이
 * 있어야만 표시됐다).
 *
 * <p>"상대방"은 보는 사람 기준이다. 내가 보낸 요청이면 수신자가, 받은 요청이면 요청자가 상대다.
 * 온보딩 전 회원이면 닉네임은 {@code null}.
 */
public record FriendResponse(
        UUID id,
        UUID requesterId,
        UUID receiverId,
        UUID counterpartId,
        String counterpartNickname,
        FriendStatus status,
        LocalDateTime createdAt,
        LocalDateTime acceptedAt
) {

    public static FriendResponse from(FriendWithCounterpart row) {
        Friend friend = row.friend();
        return new FriendResponse(
                friend.getId(),
                friend.getRequesterId(),
                friend.getReceiverId(),
                row.counterpartId(),
                row.counterpartNickname(),
                friend.getStatus(),
                friend.getCreatedAt(),
                friend.getAcceptedAt()
        );
    }

    /** 상대 이름을 아직 모르는 응답(요청 직후 등)용. 닉네임 자리는 비워 내려간다. */
    public static FriendResponse from(Friend friend, UUID viewerId) {
        UUID counterpartId = friend.getRequesterId().equals(viewerId)
                ? friend.getReceiverId() : friend.getRequesterId();
        return new FriendResponse(
                friend.getId(), friend.getRequesterId(), friend.getReceiverId(),
                counterpartId, null,
                friend.getStatus(), friend.getCreatedAt(), friend.getAcceptedAt());
    }
}
