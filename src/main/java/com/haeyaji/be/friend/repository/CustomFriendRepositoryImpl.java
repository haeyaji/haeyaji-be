package com.haeyaji.be.friend.repository;

import com.haeyaji.be.friend.domain.Friend;
import com.haeyaji.be.friend.domain.FriendStatus;
import com.haeyaji.be.friend.domain.QFriend;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CustomFriendRepositoryImpl implements CustomFriendRepository{

    private final JPAQueryFactory queryFactory;

    @Override
    public boolean existsAcceptedBetween(UUID requesterId, UUID receiverId) {

        QFriend friend = QFriend.friend;
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        booleanBuilder
                .and(friend.status.eq(FriendStatus.ACCEPTED))
                .andAnyOf(
                        friend.requesterId.eq(requesterId).and(friend.receiverId.eq(receiverId)),
                        friend.requesterId.eq(receiverId).and(friend.receiverId.eq(requesterId))
                );

        Integer result = queryFactory.selectOne()
                .from(friend)
                .where(booleanBuilder)
                .fetchFirst();

        return result != null;
    }

    @Override
    public List<Friend> findFriends(UUID memberId) {

        QFriend friend = QFriend.friend;
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        booleanBuilder
                .and(friend.status.eq(FriendStatus.ACCEPTED))
                .andAnyOf(
                        friend.requesterId.eq(memberId),
                        friend.receiverId.eq(memberId)
                );

        return queryFactory.selectFrom(friend)
                .where(booleanBuilder)
                .fetch();
    }
}
