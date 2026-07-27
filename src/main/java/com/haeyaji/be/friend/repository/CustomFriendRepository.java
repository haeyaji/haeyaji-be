package com.haeyaji.be.friend.repository;

import com.haeyaji.be.friend.domain.Friend;
import com.haeyaji.be.friend.domain.FriendStatus;
import com.haeyaji.be.friend.domain.FriendWithCounterpart;

import java.util.List;
import java.util.UUID;

public interface CustomFriendRepository {

    boolean existsAcceptedBetween(UUID requesterId, UUID receiverId);

    List<Friend> findFriends(UUID memberId);

    /** 수락된 친구 + 상대 닉네임. 이름 결합까지 한 쿼리로 끝낸다. */
    List<FriendWithCounterpart> findFriendsWithCounterpart(UUID memberId);

    /** 주고받은 요청 + 상대 닉네임. {@code sent=true}면 내가 보낸 것. */
    List<FriendWithCounterpart> findRequestsWithCounterpart(UUID memberId, FriendStatus status, boolean sent);
}
