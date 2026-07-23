package com.haeyaji.be.meeting.controller;

import com.haeyaji.be.common.response.ApiResponse;
import com.haeyaji.be.common.response.SuccessCode;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 약속 참여·초대 (MEET-4). 요청 회원은 인증 principal에서 얻는다.
 *
 * <pre>
 * POST /api/meetings/{shareToken}/participants
 * POST /api/meetings/{shareToken}/invitations
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
}
