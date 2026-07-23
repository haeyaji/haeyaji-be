package com.haeyaji.be.routine.controller;

import com.haeyaji.be.common.response.ApiResponse;
import com.haeyaji.be.common.response.SuccessCode;
import com.haeyaji.be.member.oauth.CustomUserDetails;
import com.haeyaji.be.routine.dto.RoutineApplyRequest;
import com.haeyaji.be.routine.dto.RoutineApplyResponse;
import com.haeyaji.be.routine.dto.RoutineRequest;
import com.haeyaji.be.routine.dto.RoutineResponse;
import com.haeyaji.be.routine.dto.RoutineUpdateRequest;
import com.haeyaji.be.routine.service.RoutineService;
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
 * 루틴 (FR-5, ROUT-1~4).
 *
 * <pre>
 * GET    /api/routines              목록 조회
 * GET    /api/routines/{id}         단건 조회
 * POST   /api/routines
 * PATCH  /api/routines/{id}         제목·시간·반복요일·활성여부
 * DELETE /api/routines/{id}
 * POST   /api/routines/apply        {from,to} 기간의 활성 루틴을 todo로 일괄 등록(중복 제외)
 * </pre>
 */
@RestController
@RequestMapping("/routines")
@RequiredArgsConstructor
public class RoutineController {

    private final RoutineService routineService;

    @GetMapping
    public ApiResponse<List<RoutineResponse>> getRoutines(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<RoutineResponse> routines = routineService.getRoutines(userDetails.getMemberId()).stream()
                .map(RoutineResponse::from)
                .toList();
        return ApiResponse.of(routines, SuccessCode.GET_SUCCESS);
    }

    @GetMapping("/{id}")
    public ApiResponse<RoutineResponse> getRoutine(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id
    ) {
        RoutineResponse routine = RoutineResponse.from(routineService.getRoutine(userDetails.getMemberId(), id));
        return ApiResponse.of(routine, SuccessCode.GET_SUCCESS);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RoutineResponse> createRoutine(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody RoutineRequest request
    ) {
        RoutineResponse routine = RoutineResponse.from(routineService.createRoutine(userDetails.getMemberId(), request));
        return ApiResponse.of(routine, SuccessCode.POST_SUCCESS);
    }

    @PatchMapping("/{id}")
    public ApiResponse<RoutineResponse> updateRoutine(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody RoutineUpdateRequest request
    ) {
        RoutineResponse routine = RoutineResponse.from(routineService.updateRoutine(userDetails.getMemberId(), id, request));
        return ApiResponse.of(routine, SuccessCode.PUT_SUCCESS);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRoutine(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id
    ) {
        routineService.deleteRoutine(userDetails.getMemberId(), id);
        return ApiResponse.of(null, SuccessCode.DELETE_SUCCESS);
    }

    @PostMapping("/apply")
    public ApiResponse<RoutineApplyResponse> applyRoutines(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody RoutineApplyRequest request
    ) {
        RoutineApplyResponse result = routineService.applyRoutines(userDetails.getMemberId(), request.from(), request.to());
        return ApiResponse.of(result, SuccessCode.POST_SUCCESS);
    }
}
