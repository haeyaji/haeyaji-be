package com.haeyaji.be.todo.controller;

import com.haeyaji.be.common.response.ApiResponse;
import com.haeyaji.be.common.response.SuccessCode;
import com.haeyaji.be.member.oauth.CustomUserDetails;
import com.haeyaji.be.todo.dto.TodoParticipantResponse;
import com.haeyaji.be.todo.dto.TodoParticipantRoleUpdateRequest;
import com.haeyaji.be.todo.dto.TodoResponse;
import com.haeyaji.be.todo.dto.TodoShareRequest;
import com.haeyaji.be.todo.service.TodoParticipantService;
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
 * 할 일 공유 (SHARE-1~8). {@link TodoController}와 베이스 경로(/todos)를 공유한다.
 *
 * <pre>
 * POST   /api/todos/{id}/share                    SHARE-1, owner 전용
 * POST   /api/todos/{id}/accept                    초대 응답(수락) — 본인만
 * POST   /api/todos/{id}/reject                    초대 응답(거절) — 본인만
 * GET    /api/todos/{id}/participants              SHARE-3, owner 또는 ACCEPTED 참여자
 * PATCH  /api/todos/{id}/participants/{memberId}   SHARE-4, owner 전용
 * DELETE /api/todos/{id}/participants/{memberId}   SHARE-5, owner 전용
 * POST   /api/todos/{id}/leave                     SHARE-6, 참여자 본인
 * GET    /api/todos/shared                         SHARE-8, ACCEPTED인 것만
 * </pre>
 */
@RestController
@RequestMapping("/todos")
@RequiredArgsConstructor
public class TodoParticipantController {

    private final TodoParticipantService participantService;

    @PostMapping("/{id}/share")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<TodoParticipantResponse>> share(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody TodoShareRequest request
    ) {
        var participants = participantService.share(userDetails.getMemberId(), id, request).stream()
                .map(TodoParticipantResponse::from)
                .toList();
        return ApiResponse.of(participants, SuccessCode.POST_SUCCESS);
    }

    @PostMapping("/{id}/accept")
    public ApiResponse<TodoParticipantResponse> accept(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id
    ) {
        var participant = participantService.respond(userDetails.getMemberId(), id, true);
        return ApiResponse.of(TodoParticipantResponse.from(participant), SuccessCode.PUT_SUCCESS);
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<TodoParticipantResponse> reject(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id
    ) {
        var participant = participantService.respond(userDetails.getMemberId(), id, false);
        return ApiResponse.of(TodoParticipantResponse.from(participant), SuccessCode.PUT_SUCCESS);
    }

    @GetMapping("/{id}/participants")
    public ApiResponse<List<TodoParticipantResponse>> getParticipants(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id
    ) {
        var participants = participantService.getParticipants(userDetails.getMemberId(), id).stream()
                .map(TodoParticipantResponse::from)
                .toList();
        return ApiResponse.of(participants, SuccessCode.GET_SUCCESS);
    }

    @PatchMapping("/{id}/participants/{memberId}")
    public ApiResponse<TodoParticipantResponse> changeRole(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @PathVariable UUID memberId,
            @Valid @RequestBody TodoParticipantRoleUpdateRequest request
    ) {
        var participant = participantService.changeRole(userDetails.getMemberId(), id, memberId, request.role());
        return ApiResponse.of(TodoParticipantResponse.from(participant), SuccessCode.PUT_SUCCESS);
    }

    @DeleteMapping("/{id}/participants/{memberId}")
    public ApiResponse<Void> removeParticipant(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @PathVariable UUID memberId
    ) {
        participantService.removeParticipant(userDetails.getMemberId(), id, memberId);
        return ApiResponse.of(null, SuccessCode.DELETE_SUCCESS);
    }

    @PostMapping("/{id}/leave")
    public ApiResponse<Void> leave(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id
    ) {
        participantService.leave(userDetails.getMemberId(), id);
        return ApiResponse.of(null, SuccessCode.DELETE_SUCCESS);
    }

    @GetMapping("/shared")
    public ApiResponse<List<TodoResponse>> getSharedTodos(@AuthenticationPrincipal CustomUserDetails userDetails) {
        var todos = participantService.getSharedTodos(userDetails.getMemberId()).stream()
                .map(TodoResponse::from)
                .toList();
        return ApiResponse.of(todos, SuccessCode.GET_SUCCESS);
    }
}
