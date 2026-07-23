package com.haeyaji.be.friend.controller;

import com.haeyaji.be.common.exception.BusinessException;
import com.haeyaji.be.common.exception.ErrorCode;
import com.haeyaji.be.common.response.ApiResponse;
import com.haeyaji.be.common.response.SuccessCode;
import com.haeyaji.be.friend.domain.Friend;
import com.haeyaji.be.friend.dto.FriendRequestCreateRequest;
import com.haeyaji.be.friend.dto.FriendResponse;
import com.haeyaji.be.friend.service.FriendService;
import com.haeyaji.be.member.oauth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 친구 (요청→수락 양방향).
 *
 * <pre>
 * POST   /api/friends/requests                      요청 보내기 (body: receiverId)
 * GET    /api/friends/requests?type=received|sent    받은/보낸 요청 목록 조회 (기본값 received)
 * POST   /api/friends/requests/{id}/accept
 * POST   /api/friends/requests/{id}/reject
 * GET    /api/friends                                내 친구 목록
 * DELETE /api/friends/{id}                           친구 삭제 (hard delete)
 * </pre>
 *
 * 닉네임으로 상대를 찾는 검색 API는 friend가 아니라 member 쪽 책임이라 여기 포함하지 않음
 * (필요하면 MemberController에 별도로 추가).
 */
@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @PostMapping("/requests")
    public ApiResponse<FriendResponse> sendRequest(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                    @Valid @RequestBody FriendRequestCreateRequest request) {
        Friend friend = friendService.sendRequest(userDetails.getMemberId(), request.receiverId());
        return ApiResponse.of(FriendResponse.from(friend), SuccessCode.POST_SUCCESS);
    }

    // /friends/requests?type=received 또는 ?type=sent로 같은 엔드포인트 내 분기
    @GetMapping("/requests")
    public ApiResponse<List<FriendResponse>> getRequests(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                          @RequestParam(defaultValue = "received") String type) {
        List<Friend> friends = switch (type) {
            case "received" -> friendService.getReceivedRequests(userDetails.getMemberId());
            case "sent" -> friendService.getSentRequests(userDetails.getMemberId());
            default -> throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        };

        List<FriendResponse> responses = friends.stream().map(FriendResponse::from).toList();
        return ApiResponse.of(responses, SuccessCode.GET_SUCCESS);
    }

    @PostMapping("/requests/{id}/accept")
    public ApiResponse<FriendResponse> acceptRequest(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                      @PathVariable UUID id) {
        Friend friend = friendService.acceptRequest(id, userDetails.getMemberId());
        return ApiResponse.of(FriendResponse.from(friend), SuccessCode.PUT_SUCCESS);
    }

    @PostMapping("/requests/{id}/reject")
    public ApiResponse<FriendResponse> rejectRequest(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                      @PathVariable UUID id) {
        Friend friend = friendService.rejectRequest(id, userDetails.getMemberId());
        return ApiResponse.of(FriendResponse.from(friend), SuccessCode.PUT_SUCCESS);
    }

    @GetMapping
    public ApiResponse<List<FriendResponse>> getFriends(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<FriendResponse> responses = friendService.getFriends(userDetails.getMemberId()).stream()
                .map(FriendResponse::from)
                .toList();
        return ApiResponse.of(responses, SuccessCode.GET_SUCCESS);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteFriend(@AuthenticationPrincipal CustomUserDetails userDetails,
                                           @PathVariable UUID id) {
        friendService.deleteFriend(id, userDetails.getMemberId());
        return ApiResponse.of(null, SuccessCode.DELETE_SUCCESS);
    }
}
