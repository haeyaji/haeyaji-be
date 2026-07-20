package com.haeyaji.be.todo.dto;

import com.haeyaji.be.todo.domain.TodoCount;

public record TodoCountResponse(int total, int completed) {

    public static TodoCountResponse from(TodoCount count) {
        return new TodoCountResponse(count.total(), count.completed());
    }
}
