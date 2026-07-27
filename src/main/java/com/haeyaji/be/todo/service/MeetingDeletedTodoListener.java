package com.haeyaji.be.todo.service;

import com.haeyaji.be.meeting.domain.MeetingDeletedEvent;
import com.haeyaji.be.todo.domain.TodoSource;
import com.haeyaji.be.todo.repository.TodoEntity;
import com.haeyaji.be.todo.repository.TodoParticipantRepository;
import com.haeyaji.be.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * 약속 삭제 → 그 약속에서 생긴 공유 할 일 회수 (MEET-16).
 *
 * <p>확정 시 만든 할 일({@link MeetingConfirmedTodoListener})은 참여자 캘린더에도 퍼져 있다.
 * 약속만 지우면 아무도 그 일정이 왜 있는지 설명할 수 없는 유령으로 남으므로 함께 지운다.
 *
 * <p>확정 처리와 같은 이유로 같은 트랜잭션에서 돈다 — 약속은 사라졌는데 할 일만 남는 중간 상태를
 * 만들지 않기 위해서다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingDeletedTodoListener {

    private final TodoRepository todoRepository;
    private final TodoParticipantRepository participantRepository;

    @EventListener
    public void removeSharedTodo(MeetingDeletedEvent event) {
        List<TodoEntity> derived =
                todoRepository.findBySourceAndSourceRefId(TodoSource.MEETING, event.meetingId());
        if (derived.isEmpty()) {
            return;
        }
        for (TodoEntity todo : derived) {
            participantRepository.deleteByTodoId(todo.getId()); // FK가 없어 참여자 행이 고아로 남는다
        }
        todoRepository.deleteAll(derived);
        log.info("약속 삭제 → 공유 할 일 회수: meetingId={} 할일={}건", event.meetingId(), derived.size());
    }
}
