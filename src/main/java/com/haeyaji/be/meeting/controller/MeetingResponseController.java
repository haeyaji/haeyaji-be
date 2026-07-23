package com.haeyaji.be.meeting.controller;

import com.haeyaji.be.common.response.ApiResponse;
import com.haeyaji.be.common.response.SuccessCode;
import com.haeyaji.be.meeting.dto.BestTimeResponse;
import com.haeyaji.be.meeting.dto.HeatmapResponse;
import com.haeyaji.be.meeting.dto.MeetingStatusResponse;
import com.haeyaji.be.meeting.dto.ResponseSubmitRequest;
import com.haeyaji.be.meeting.dto.SlotResponseItem;
import com.haeyaji.be.meeting.service.MeetingResponseService;
import com.haeyaji.be.member.oauth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 시간 응답 제출·집계 (MEET-5·6·7·8).
 *
 * <pre>
 * PUT /api/meetings/{shareToken}/responses
 * GET /api/meetings/{shareToken}/heatmap
 * GET /api/meetings/{shareToken}/best-times
 * GET /api/meetings/{shareToken}/status
 * </pre>
 */
@RestController
@RequestMapping("/meetings")
@RequiredArgsConstructor
public class MeetingResponseController {

    private final MeetingResponseService meetingResponseService;

    @PutMapping("/{shareToken}/responses")
    public ResponseEntity<ApiResponse<List<SlotResponseItem>>> submitResponses(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String shareToken,
            @Valid @RequestBody ResponseSubmitRequest request
    ) {
        List<SlotResponseItem> responses = meetingResponseService
                .submit(shareToken, userDetails.getMemberId(), request).stream()
                .map(SlotResponseItem::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.of(responses, SuccessCode.PUT_SUCCESS));
    }

    @GetMapping("/{shareToken}/heatmap")
    public ResponseEntity<ApiResponse<HeatmapResponse>> getHeatmap(@PathVariable String shareToken) {
        HeatmapResponse heatmap = HeatmapResponse.from(meetingResponseService.getAvailability(shareToken));
        return ResponseEntity.ok(ApiResponse.of(heatmap, SuccessCode.GET_SUCCESS));
    }

    @GetMapping("/{shareToken}/best-times")
    public ResponseEntity<ApiResponse<BestTimeResponse>> getBestTimes(@PathVariable String shareToken) {
        BestTimeResponse bestTimes = BestTimeResponse.from(meetingResponseService.getBestWindows(shareToken));
        return ResponseEntity.ok(ApiResponse.of(bestTimes, SuccessCode.GET_SUCCESS));
    }

    @GetMapping("/{shareToken}/status")
    public ResponseEntity<ApiResponse<MeetingStatusResponse>> getStatus(@PathVariable String shareToken) {
        MeetingStatusResponse status = MeetingStatusResponse.from(
                meetingResponseService.getResponseBoard(shareToken));
        return ResponseEntity.ok(ApiResponse.of(status, SuccessCode.GET_SUCCESS));
    }
}
