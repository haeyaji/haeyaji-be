package com.haeyaji.be.todo.controller;

import com.haeyaji.be.todo.dto.TodoRequest;
import com.haeyaji.be.todo.dto.TodoResponse;
import com.haeyaji.be.todo.service.TodoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
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
 * GET  /api/todos?date={yyyy-MM-dd}
 * POST /api/todos
 * </pre>
 */
@RestController
@RequestMapping("/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    @GetMapping
    public List<TodoResponse> getTodos(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return todoService.getTodosByDate(date).stream()
                .map(TodoResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TodoResponse createTodo(@Valid @RequestBody TodoRequest request) {
        return TodoResponse.from(todoService.createTodo(request));
    }
}
