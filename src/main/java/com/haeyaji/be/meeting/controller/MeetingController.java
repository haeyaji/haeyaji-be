package com.haeyaji.be.meeting.controller;

import com.haeyaji.be.common.response.ApiResponse;
import com.haeyaji.be.common.response.SuccessCode;
import com.haeyaji.be.meeting.dto.MeetingConfirmRequest;
import com.haeyaji.be.meeting.dto.MeetingCreateRequest;
import com.haeyaji.be.meeting.dto.MeetingDetailResponse;
import com.haeyaji.be.meeting.dto.MeetingSummaryResponse;
import com.haeyaji.be.meeting.service.MeetingService;
import com.haeyaji.be.member.oauth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 약속 생성·조회·확정 (MEET-1·2·9·11·12·13·14). 요청 회원은 인증 principal에서 얻는다.
 *
 * <pre>
 * POST  /api/meetings
 * GET   /api/meetings
 * GET   /api/meetings/{shareToken}
 * PATCH /api/meetings/{shareToken}/confirm
 * </pre>
 */
@RestController
@RequestMapping("/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping
    public ResponseEntity<ApiResponse<MeetingDetailResponse>> createMeeting(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MeetingCreateRequest request
    ) {
        MeetingDetailResponse meeting = MeetingDetailResponse.from(
                meetingService.create(userDetails.getMemberId(), request));
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.of(meeting, SuccessCode.POST_SUCCESS));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MeetingSummaryResponse>>> getMeetings(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<MeetingSummaryResponse> meetings = meetingService.listByMember(userDetails.getMemberId()).stream()
                .map(MeetingSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.of(meetings, SuccessCode.GET_SUCCESS));
    }

    @GetMapping("/{shareToken}")
    public ResponseEntity<ApiResponse<MeetingDetailResponse>> getMeeting(@PathVariable String shareToken) {
        MeetingDetailResponse meeting = MeetingDetailResponse.from(meetingService.getByShareToken(shareToken));
        return ResponseEntity.ok(ApiResponse.of(meeting, SuccessCode.GET_SUCCESS));
    }

    @PatchMapping("/{shareToken}/confirm")
    public ResponseEntity<ApiResponse<MeetingDetailResponse>> confirmMeeting(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String shareToken,
            @Valid @RequestBody MeetingConfirmRequest request
    ) {
        MeetingDetailResponse meeting = MeetingDetailResponse.from(
                meetingService.confirm(shareToken, userDetails.getMemberId(), request));
        return ResponseEntity.ok(ApiResponse.of(meeting, SuccessCode.PUT_SUCCESS));
    }
}
