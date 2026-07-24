package com.haeyaji.be.routine.service;

import com.haeyaji.be.todo.repository.TodoEntity;
import com.haeyaji.be.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * apply 배치 중 건별 저장을 호출자의 트랜잭션과 분리한다(H1).
 * {@code saveAndFlush}가 유니크 제약 위반으로 실패하면 Hibernate 세션이 rollback-only로
 * 마킹되는데, 이 메서드 안에서 예외를 잡아버려도 소용없다 — 커밋을 시도하는 순간
 * 여전히 UnexpectedRollbackException이 터진다(격리 대상이 바깥 트랜잭션에서 이 트랜잭션
 * 자신으로 옮겨질 뿐). 그래서 여기선 절대 catch하지 않고 그대로 던진다: REQUIRES_NEW라
 * Spring이 이 메서드의 트랜잭션만 정상적으로 롤백 처리하고, 그 후 예외가 호출자(바깥
 * 트랜잭션, 이 실패와 무관한 자신의 세션을 그대로 유지 중)로 전파되어 거기서 안전하게
 * catch할 수 있다.
 */
@Component
@RequiredArgsConstructor
class RoutineTodoWriter {

    private final TodoRepository todoRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(TodoEntity todo) {
        todoRepository.saveAndFlush(todo);
    }
}
