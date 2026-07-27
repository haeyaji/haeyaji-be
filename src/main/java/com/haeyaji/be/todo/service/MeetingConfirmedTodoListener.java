package com.haeyaji.be.todo.service;

import com.haeyaji.be.meeting.domain.MeetingConfirmedEvent;
import com.haeyaji.be.todo.domain.ParticipantRole;
import com.haeyaji.be.todo.domain.TodoSource;
import com.haeyaji.be.todo.repository.TodoEntity;
import com.haeyaji.be.todo.repository.TodoParticipantEntity;
import com.haeyaji.be.todo.repository.TodoParticipantRepository;
import com.haeyaji.be.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 약속 확정 → 공유 할 일 전환 (MEET-10).
 * <p>약속이 확정되면 확정 시각으로 할 일 1건을 만들고(소유자=생성자), 나머지 참여자를 EDITOR로 공유한다.
 * 참여자는 이미 약속에 합류하며 의사를 밝혔으므로 다시 수락을 요구하지 않고 바로 ACCEPTED로 넣는다.
 * <p>이벤트로 구독해 meeting 모듈이 todo 모듈을 몰라도 되게 한다. 확정과 같은 트랜잭션에서 처리해
 * "확정됐는데 할 일이 없는" 중간 상태를 만들지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingConfirmedTodoListener {

    private final TodoRepository todoRepository;
    private final TodoParticipantRepository participantRepository;

    @EventListener
    public void createSharedTodo(MeetingConfirmedEvent event) {
        LocalDate todoDate = event.confirmedStartAt().toLocalDate();
        // 멱등: 같은 약속으로 이미 만들었으면 건너뛴다(재확정·이벤트 재발행 대비).
        if (todoRepository.existsByTodoDateAndSourceAndSourceRefId(
                todoDate, TodoSource.MEETING, event.meetingId())) {
            return;
        }

        TodoEntity todo = todoRepository.save(TodoEntity.createFromMeeting(
                event.creatorId(),
                event.meetingTitle(),
                todoDate,
                event.confirmedStartAt().toLocalTime(),
                event.meetingId()));

        List<TodoParticipantEntity> shares = event.participantMemberIds().stream()
                .filter(memberId -> !memberId.equals(event.creatorId())) // 소유자는 todo.member_id로 표현
                .distinct()
                .map(memberId -> acceptedEditor(todo.getId(), memberId))
                .toList();
        if (!shares.isEmpty()) {
            participantRepository.saveAll(shares);
        }
        log.info("약속 확정 → 공유 할 일 생성: meetingId={} todoId={} 공유대상={}명",
                event.meetingId(), todo.getId(), shares.size());
    }

    private static TodoParticipantEntity acceptedEditor(UUID todoId, UUID memberId) {
        TodoParticipantEntity participant =
                TodoParticipantEntity.invite(todoId, memberId, ParticipantRole.EDITOR);
        participant.accept();
        return participant;
    }
}
