package com.haeyaji.be.label.controller;

import com.haeyaji.be.common.response.ApiResponse;
import com.haeyaji.be.common.response.SuccessCode;
import com.haeyaji.be.label.dto.LabelRequest;
import com.haeyaji.be.label.dto.LabelResponse;
import com.haeyaji.be.label.dto.LabelUpdateRequest;
import com.haeyaji.be.label.service.LabelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public ApiResponse<List<LabelResponse>> getLabels() {
        List<LabelResponse> labels = labelService.getLabels().stream()
                .map(LabelResponse::from)
                .toList();
        return ApiResponse.of(labels, SuccessCode.GET_SUCCESS);
    }

    @GetMapping("/{id}")
    public ApiResponse<LabelResponse> getLabel(@PathVariable UUID id) {
        LabelResponse label = LabelResponse.from(labelService.getLabel(id));
        return ApiResponse.of(label, SuccessCode.GET_SUCCESS);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LabelResponse> createLabel(@Valid @RequestBody LabelRequest request) {
        LabelResponse label = LabelResponse.from(labelService.createLabel(request));
        return ApiResponse.of(label, SuccessCode.POST_SUCCESS);
    }

    @PatchMapping("/{id}")
    public ApiResponse<LabelResponse> updateLabel(
            @PathVariable UUID id,
            @Valid @RequestBody LabelUpdateRequest request
    ) {
        LabelResponse label = LabelResponse.from(labelService.updateLabel(id, request));
        return ApiResponse.of(label, SuccessCode.PUT_SUCCESS);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteLabel(@PathVariable UUID id) {
        labelService.deleteLabel(id);
        return ApiResponse.of(null, SuccessCode.DELETE_SUCCESS);
    }
}
