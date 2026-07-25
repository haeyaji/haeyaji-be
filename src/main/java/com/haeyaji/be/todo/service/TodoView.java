package com.haeyaji.be.todo.service;

import com.haeyaji.be.todo.domain.ParticipantRole;
import com.haeyaji.be.todo.domain.Todo;

/**
 * 캘린더(날짜별) 조회 결과 한 건. 내 소유 할 일과 공유받은(ACCEPTED) 할 일을 한 목록에 합쳐 반환한다.
 *
 * @param todo       할 일
 * @param sharedRole {@code null}이면 내 소유, 아니면 공유받은 것이고 내 권한(EDITOR/VIEWER)
 */
public record TodoView(Todo todo, ParticipantRole sharedRole) {

    public static TodoView owned(Todo todo) {
        return new TodoView(todo, null);
    }

    public static TodoView shared(Todo todo, ParticipantRole role) {
        return new TodoView(todo, role);
    }
}
