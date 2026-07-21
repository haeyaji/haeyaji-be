package com.haeyaji.be.todo.dto;

import java.util.List;

/**
 * 선택 날짜의 할 일 목록 + 완료/전체 개수 집계 (한 응답에 같이 반환).
 */
public record TodoListResponse(
        List<TodoResponse> tasks,
        int total,
        int completed
) {

    public static TodoListResponse from(List<TodoResponse> tasks) {
        int completedCount = (int) tasks.stream().filter(TodoResponse::completed).count();
        return new TodoListResponse(tasks, tasks.size(), completedCount);
    }
}
