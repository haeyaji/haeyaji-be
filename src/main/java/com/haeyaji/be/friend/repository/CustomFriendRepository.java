package com.haeyaji.be.friend.repository;

import com.haeyaji.be.friend.domain.Friend;

import java.util.List;
import java.util.UUID;

public interface CustomFriendRepository {

    boolean existsAcceptedBetween(UUID requesterId, UUID receiverId);

    List<Friend> findFriends(UUID memberId);
}
