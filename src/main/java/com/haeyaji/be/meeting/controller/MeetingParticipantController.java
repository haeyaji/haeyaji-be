package com.haeyaji.be.meeting.controller;

import com.haeyaji.be.common.response.ApiResponse;
import com.haeyaji.be.common.response.SuccessCode;
import com.haeyaji.be.meeting.dto.ParticipantResponse;
import com.haeyaji.be.meeting.service.MeetingParticipationService;
import com.haeyaji.be.member.oauth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 약속 참여 (MEET-4). 참여 회원은 인증 principal에서 얻으며, 요청 본문은 없다.
 *
 * <pre>
 * POST /api/meetings/{shareToken}/participants
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
}
