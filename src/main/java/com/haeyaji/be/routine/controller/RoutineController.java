package com.haeyaji.be.routine.controller;

import com.haeyaji.be.common.response.ApiResponse;
import com.haeyaji.be.common.response.SuccessCode;
import com.haeyaji.be.routine.dto.RoutineRequest;
import com.haeyaji.be.routine.dto.RoutineResponse;
import com.haeyaji.be.routine.dto.RoutineUpdateRequest;
import com.haeyaji.be.routine.service.RoutineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 루틴 (FR-5, ROUT-1~3).
 *
 * <pre>
 * POST   /api/routines
 * PATCH  /api/routines/{id}   제목·시간·반복요일·활성여부
 * DELETE /api/routines/{id}
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

    @PatchMapping("/{id}")
    public ApiResponse<RoutineResponse> updateRoutine(
            @PathVariable UUID id,
            @Valid @RequestBody RoutineUpdateRequest request
    ) {
        RoutineResponse routine = RoutineResponse.from(routineService.updateRoutine(id, request));
        return ApiResponse.of(routine, SuccessCode.PUT_SUCCESS);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRoutine(@PathVariable UUID id) {
        routineService.deleteRoutine(id);
        return ApiResponse.of(null, SuccessCode.DELETE_SUCCESS);
    }
}
