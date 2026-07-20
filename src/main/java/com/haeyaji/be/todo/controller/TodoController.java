package com.haeyaji.be.todo.controller;

import com.haeyaji.be.common.response.ApiResponse;
import com.haeyaji.be.common.response.SuccessCode;
import com.haeyaji.be.todo.dto.TodoRequest;
import com.haeyaji.be.todo.dto.TodoResponse;
import com.haeyaji.be.todo.dto.TodoUpdateRequest;
import com.haeyaji.be.todo.service.TodoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
import java.util.List;

/**
 * 할 일 (FR-1).
 *
 * <pre>
 * GET   /api/todos?date={yyyy-MM-dd}
 * POST  /api/todos
 * PATCH /api/todos/{id}
 * PATCH /api/todos/{id}/toggle
 * </pre>
 */
@RestController
@RequestMapping("/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    @GetMapping
    public ApiResponse<List<TodoResponse>> getTodos(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<TodoResponse> todos = todoService.getTodosByDate(date).stream()
                .map(TodoResponse::from)
                .toList();
        return ApiResponse.of(todos, SuccessCode.GET_SUCCESS);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TodoResponse> createTodo(@Valid @RequestBody TodoRequest request) {
        TodoResponse todo = TodoResponse.from(todoService.createTodo(request));
        return ApiResponse.of(todo, SuccessCode.POST_SUCCESS);
    }

    @PatchMapping("/{id}")
    public ApiResponse<TodoResponse> updateTodo(
            @PathVariable Long id,
            @Valid @RequestBody TodoUpdateRequest request
    ) {
        TodoResponse todo = TodoResponse.from(todoService.updateTodo(id, request));
        return ApiResponse.of(todo, SuccessCode.PUT_SUCCESS);
    }

    @PatchMapping("/{id}/toggle")
    public ApiResponse<TodoResponse> toggleTodo(@PathVariable Long id) {
        TodoResponse todo = TodoResponse.from(todoService.toggleTodo(id));
        return ApiResponse.of(todo, SuccessCode.PUT_SUCCESS);
    }
}
