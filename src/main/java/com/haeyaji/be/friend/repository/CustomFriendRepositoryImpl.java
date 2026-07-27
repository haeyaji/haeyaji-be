package com.haeyaji.be.friend.repository;

import com.haeyaji.be.friend.domain.Friend;
import com.haeyaji.be.friend.domain.FriendStatus;
import com.haeyaji.be.friend.domain.FriendWithCounterpart;
import com.haeyaji.be.friend.domain.QFriend;
import com.haeyaji.be.member.domain.QMember;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.ComparableExpression;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQuery;
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

    @Override
    public List<FriendWithCounterpart> findFriendsWithCounterpart(UUID memberId) {
        QFriend friend = QFriend.friend;
        return selectWithCounterpart(friend, memberId)
                .where(friend.status.eq(FriendStatus.ACCEPTED)
                        .and(friend.requesterId.eq(memberId).or(friend.receiverId.eq(memberId))))
                .fetch();
    }

    @Override
    public List<FriendWithCounterpart> findRequestsWithCounterpart(UUID memberId, FriendStatus status, boolean sent) {
        QFriend friend = QFriend.friend;
        return selectWithCounterpart(friend, memberId)
                .where(friend.status.eq(status)
                        .and(sent ? friend.requesterId.eq(memberId) : friend.receiverId.eq(memberId)))
                .fetch();
    }

    /**
     * 관계 행 + 상대 회원을 붙여 한 번에 읽는다.
     *
     * <p>{@code friend}엔 회원 연관관계가 없고 id만 있어서(프로젝트 규약) 연관 조인을 못 쓴다.
     * 대신 "보는 사람이 아닌 쪽"을 CASE로 골라 그 id로 회원에 조인한다.
     *
     * <p>탈퇴 등으로 회원 행이 없어도 관계는 보여야 하므로 left join이다(닉네임만 null).
     */
    private JPAQuery<FriendWithCounterpart> selectWithCounterpart(QFriend friend, UUID memberId) {
        QMember member = QMember.member;
        ComparableExpression<UUID> counterpartId = new CaseBuilder()
                .when(friend.requesterId.eq(memberId)).then(friend.receiverId)
                .otherwise(friend.requesterId);
        return queryFactory
                .select(Projections.constructor(FriendWithCounterpart.class,
                        friend, counterpartId, member.nickname))
                .from(friend)
                .leftJoin(member).on(member.id.eq(counterpartId));
    }
}
