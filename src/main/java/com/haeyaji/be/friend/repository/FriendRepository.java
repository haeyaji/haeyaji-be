package com.haeyaji.be.friend.repository;

import com.haeyaji.be.friend.domain.Friend;
import com.haeyaji.be.friend.domain.FriendStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendRepository extends JpaRepository<Friend, UUID>, CustomFriendRepository {

    Optional<Friend> findByRequesterIdAndReceiverIdAndStatus(UUID requesterId, UUID receiverId, FriendStatus status);

    List<Friend> findByReceiverIdAndStatus(UUID memberId, FriendStatus friendStatus);

    List<Friend> findByRequesterIdAndStatus(UUID memberId, FriendStatus friendStatus);
}
