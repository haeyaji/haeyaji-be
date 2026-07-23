package com.haeyaji.be.label.controller;

import com.haeyaji.be.common.response.ApiResponse;
import com.haeyaji.be.common.response.SuccessCode;
import com.haeyaji.be.label.dto.LabelRequest;
import com.haeyaji.be.label.dto.LabelResponse;
import com.haeyaji.be.label.dto.LabelUpdateRequest;
import com.haeyaji.be.label.service.LabelService;
import com.haeyaji.be.member.oauth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 라벨 (LABEL-1~3).
 *
 * <pre>
 * GET    /api/labels          목록 조회
 * GET    /api/labels/{id}     단건 조회
 * POST   /api/labels
 * PATCH  /api/labels/{id}     이름·색상
 * DELETE /api/labels/{id}
 * </pre>
 */
@RestController
@RequestMapping("/labels")
@RequiredArgsConstructor
public class LabelController {

    private final LabelService labelService;

    @GetMapping
    public ApiResponse<List<LabelResponse>> getLabels(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<LabelResponse> labels = labelService.getLabels(userDetails.getMemberId()).stream()
                .map(LabelResponse::from)
                .toList();
        return ApiResponse.of(labels, SuccessCode.GET_SUCCESS);
    }

    @GetMapping("/{id}")
    public ApiResponse<LabelResponse> getLabel(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id
    ) {
        LabelResponse label = LabelResponse.from(labelService.getLabel(userDetails.getMemberId(), id));
        return ApiResponse.of(label, SuccessCode.GET_SUCCESS);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LabelResponse> createLabel(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody LabelRequest request
    ) {
        LabelResponse label = LabelResponse.from(labelService.createLabel(userDetails.getMemberId(), request));
        return ApiResponse.of(label, SuccessCode.POST_SUCCESS);
    }

    @PatchMapping("/{id}")
    public ApiResponse<LabelResponse> updateLabel(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody LabelUpdateRequest request
    ) {
        LabelResponse label = LabelResponse.from(labelService.updateLabel(userDetails.getMemberId(), id, request));
        return ApiResponse.of(label, SuccessCode.PUT_SUCCESS);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteLabel(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id
    ) {
        labelService.deleteLabel(userDetails.getMemberId(), id);
        return ApiResponse.of(null, SuccessCode.DELETE_SUCCESS);
    }
}
