package com.haeyaji.be.todo.domain;

/**
 * 선택 날짜의 할 일 완료/전체 개수 집계.
 */
public record TodoCount(int total, int completed) {
}
