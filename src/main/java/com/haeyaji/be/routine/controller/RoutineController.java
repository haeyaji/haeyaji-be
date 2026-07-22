package com.haeyaji.be.routine.controller;

import com.haeyaji.be.common.response.ApiResponse;
import com.haeyaji.be.common.response.SuccessCode;
import com.haeyaji.be.routine.dto.RoutineRequest;
import com.haeyaji.be.routine.dto.RoutineResponse;
import com.haeyaji.be.routine.service.RoutineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 루틴 (FR-5, ROUT-1).
 *
 * <pre>
 * POST /api/routines
 * </pre>
 */
@RestController
@RequestMapping("/routines")
@RequiredArgsConstructor
public class RoutineController {

    private final RoutineService routineService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RoutineResponse> createRoutine(@Valid @RequestBody RoutineRequest request) {
        RoutineResponse routine = RoutineResponse.from(routineService.createRoutine(request));
        return ApiResponse.of(routine, SuccessCode.POST_SUCCESS);
    }
}
