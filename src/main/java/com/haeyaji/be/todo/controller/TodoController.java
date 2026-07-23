package com.haeyaji.be.todo.controller;

import com.haeyaji.be.common.response.ApiResponse;
import com.haeyaji.be.common.response.SuccessCode;
import com.haeyaji.be.member.oauth.CustomUserDetails;
import com.haeyaji.be.todo.dto.TodoListResponse;
import com.haeyaji.be.todo.dto.TodoRequest;
import com.haeyaji.be.todo.dto.TodoResponse;
import com.haeyaji.be.todo.dto.TodoUpdateRequest;
import com.haeyaji.be.todo.service.TodoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 할 일 (FR-1).
 *
 * <pre>
 * GET    /api/todos?date={yyyy-MM-dd}   목록 + 완료/전체 집계
 * POST   /api/todos
 * PATCH  /api/todos/{id}                제목·시간·장소·분류·완료 여부
 * DELETE /api/todos/{id}
 * </pre>
 */
@RestController
@RequestMapping("/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    @GetMapping
    public ApiResponse<TodoListResponse> getTodos(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        var tasks = todoService.getTodosByDate(userDetails.getMemberId(), date).stream()
                .map(TodoResponse::from)
                .toList();
        return ApiResponse.of(TodoListResponse.from(tasks), SuccessCode.GET_SUCCESS);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TodoResponse> createTodo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody TodoRequest request
    ) {
        TodoResponse todo = TodoResponse.from(todoService.createTodo(userDetails.getMemberId(), request));
        return ApiResponse.of(todo, SuccessCode.POST_SUCCESS);
    }

    @PatchMapping("/{id}")
    public ApiResponse<TodoResponse> updateTodo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody TodoUpdateRequest request
    ) {
        TodoResponse todo = TodoResponse.from(todoService.updateTodo(userDetails.getMemberId(), id, request));
        return ApiResponse.of(todo, SuccessCode.PUT_SUCCESS);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTodo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id
    ) {
        todoService.deleteTodo(userDetails.getMemberId(), id);
        return ApiResponse.of(null, SuccessCode.DELETE_SUCCESS);
    }
}
