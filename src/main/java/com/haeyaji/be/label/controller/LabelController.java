package com.haeyaji.be.label.controller;

import com.haeyaji.be.common.response.ApiResponse;
import com.haeyaji.be.common.response.SuccessCode;
import com.haeyaji.be.label.dto.LabelRequest;
import com.haeyaji.be.label.dto.LabelResponse;
import com.haeyaji.be.label.service.LabelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 라벨 (LABEL-1).
 *
 * <pre>
 * POST /api/labels
 * </pre>
 */
@RestController
@RequestMapping("/labels")
@RequiredArgsConstructor
public class LabelController {

    private final LabelService labelService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LabelResponse> createLabel(@Valid @RequestBody LabelRequest request) {
        LabelResponse label = LabelResponse.from(labelService.createLabel(request));
        return ApiResponse.of(label, SuccessCode.POST_SUCCESS);
    }
}
