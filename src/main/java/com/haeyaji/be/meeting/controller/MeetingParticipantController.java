package com.haeyaji.be.meeting.controller;

import com.haeyaji.be.common.response.ApiResponse;
import com.haeyaji.be.common.response.SuccessCode;
import com.haeyaji.be.meeting.dto.MeetingInvitationResponse;
import com.haeyaji.be.meeting.dto.MeetingInviteRequest;
import com.haeyaji.be.meeting.dto.MeetingInviteResponse;
import com.haeyaji.be.meeting.dto.ParticipantResponse;
import com.haeyaji.be.meeting.service.MeetingParticipationService;
import com.haeyaji.be.member.oauth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 약속 참여·초대 (MEET-4). 요청 회원은 인증 principal에서 얻는다.
 *
 * <pre>
 * POST /api/meetings/{shareToken}/participants   합류(=초대 수락). 이미 수락했으면 멱등
 * POST /api/meetings/{shareToken}/invitations    친구 초대 → PENDING 행 + 알림 이벤트
 * GET  /api/meetings/invitations                 내가 받은 대기 초대함(알림 유실 대비 조회 경로)
 * POST /api/meetings/{shareToken}/reject         받은 초대 거절
 * </pre>
 */
@RestController
@RequestMapping("/meetings")
@RequiredArgsConstructor
public class MeetingParticipantController {

    private final MeetingParticipationService meetingParticipationService;

    @PostMapping("/{shareToken}/participants")
    public ResponseEntity<ApiResponse<ParticipantResponse>> join(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String shareToken
    ) {
        ParticipantResponse participant = ParticipantResponse.from(
                meetingParticipationService.join(shareToken, userDetails.getMemberId()));
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.of(participant, SuccessCode.POST_SUCCESS));
    }

    @PostMapping("/{shareToken}/invitations")
    public ResponseEntity<ApiResponse<MeetingInviteResponse>> invite(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String shareToken,
            @Valid @RequestBody MeetingInviteRequest request
    ) {
        MeetingInviteResponse result = MeetingInviteResponse.from(
                meetingParticipationService.invite(shareToken, userDetails.getMemberId(), request.memberIds()));
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.of(result, SuccessCode.POST_SUCCESS));
    }

    /** 받은 대기(PENDING) 약속 초대 목록. 알림이 없거나 지워져도 여기서 초대를 찾을 수 있다. */
    @GetMapping("/invitations")
    public ApiResponse<List<MeetingInvitationResponse>> getPendingInvitations(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<MeetingInvitationResponse> invitations =
                meetingParticipationService.getPendingInvitations(userDetails.getMemberId()).stream()
                        .map(MeetingInvitationResponse::from)
                        .toList();
        return ApiResponse.of(invitations, SuccessCode.GET_SUCCESS);
    }

    /** 받은 초대 거절(수락은 합류 API가 겸한다). 거절 후에도 재초대될 수 있다. */
    @PostMapping("/{shareToken}/reject")
    public ApiResponse<Void> rejectInvitation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String shareToken
    ) {
        meetingParticipationService.rejectInvitation(shareToken, userDetails.getMemberId());
        return ApiResponse.of(null, SuccessCode.PUT_SUCCESS);
    }
}
