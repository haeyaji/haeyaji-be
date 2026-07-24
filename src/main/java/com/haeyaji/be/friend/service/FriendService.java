package com.haeyaji.be.friend.service;

import com.haeyaji.be.common.exception.BusinessException;
import com.haeyaji.be.common.exception.ErrorCode;
import com.haeyaji.be.friend.domain.Friend;
import com.haeyaji.be.friend.domain.FriendStatus;
import com.haeyaji.be.friend.repository.FriendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendService {

    private final FriendRepository friendRepository;

    @Transactional
    public Friend sendRequest(UUID requesterId, UUID receiverId) {

        // 1) 자신에게 친구 요청 보내면 예외
        if (requesterId.equals(receiverId)) {
            throw new BusinessException(ErrorCode.SELF_FRIEND_REQUEST_NOT_ALLOWED);
        }

        // 2) 이미 친구 관계인지 확인
        if (friendRepository.existsAcceptedBetween(requesterId, receiverId)) {
            throw new BusinessException(ErrorCode.ALREADY_FRIENDS);
        }

        // 3) 상대방이 보낸 친구 요청이 있으면 새로 만들지 않고 accept
        Optional<Friend> reversePending = friendRepository
                .findByRequesterIdAndReceiverIdAndStatus(receiverId, requesterId, FriendStatus.PENDING);

        if (reversePending.isPresent()) {
            Friend existing = reversePending.get();
            existing.accept();
            return existing;
        }

        // 4) 같은 방향으로 예전에 거절된 row가 있으면(소프트 삭제 방식이라 REJECTED row가 계속 남아있음)
        // 새로 INSERT하지 않고 그 row를 재사용해서 PENDING으로 되돌림 — 안 그러면 uk_friend_pair 유니크 제약과 충돌함
        Optional<Friend> rejectedBefore = friendRepository
                .findByRequesterIdAndReceiverIdAndStatus(requesterId, receiverId, FriendStatus.REJECTED);

        if (rejectedBefore.isPresent()) {
            Friend existing = rejectedBefore.get();
            existing.resend();
            return existing;
        }

        // 5) 그 외엔 새로 생성
        Friend friend = Friend.create(requesterId, receiverId);

        try {
            return friendRepository.saveAndFlush(friend);   // saveAndFlush로 즉시 INSERT 실행
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 요청이 두 번 들어온 경우 unique 위반: 500 에러 감싸기
            throw new BusinessException(ErrorCode.DUPLICATE_FRIEND_REQUEST);
        }
    }

    // 친구 요청 수락
    @Transactional
    public Friend acceptRequest(UUID friendId, UUID memberId) {

        Friend friend = friendRepository.findById(friendId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        if (!friend.getReceiverId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_FORBIDDEN);
        }

        if (friend.getStatus() != FriendStatus.PENDING) {
            throw new BusinessException(ErrorCode.REQUEST_ALREADY_PROCESSED);
        }

        friend.accept();

        return friend;
    }

    // 친구 요청 거절
    @Transactional
    public Friend rejectRequest(UUID friendId, UUID memberId) {

        Friend friend = friendRepository.findById(friendId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        if (!friend.getReceiverId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_FORBIDDEN);
        }

        if (friend.getStatus() != FriendStatus.PENDING) {
            throw new BusinessException(ErrorCode.REQUEST_ALREADY_PROCESSED);
        }

        friend.reject();

        return friend;
    }

    // 내 친구 목록
    public List<Friend> getFriends(UUID memberId) {
        return friendRepository.findFriends(memberId);
    }

    // 내가 받은 친구 요청 목록
    public List<Friend> getReceivedRequests(UUID memberId) {
        return friendRepository.findByReceiverIdAndStatus(memberId, FriendStatus.PENDING);
    }

    /**
     * 내가 보낸 친구 요청 목록 (status=PENDING).
     * TODO friendRepository.findByRequesterIdAndStatus(memberId, PENDING)
     */
    public List<Friend> getSentRequests(UUID memberId) {
        return friendRepository.findByRequesterIdAndStatus(memberId, FriendStatus.PENDING);
    }
    
    @Transactional
    public void deleteFriend(UUID friendId, UUID memberId) {

        Friend friend = friendRepository.findById(friendId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        if (!(friend.getRequesterId().equals(memberId) || friend.getReceiverId().equals(memberId))) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_FORBIDDEN);
        }

        friendRepository.delete(friend);
    }
}
