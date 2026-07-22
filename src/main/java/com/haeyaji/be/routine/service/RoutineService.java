package com.haeyaji.be.routine.service;

import com.haeyaji.be.common.exception.BusinessException;
import com.haeyaji.be.common.exception.ErrorCode;
import com.haeyaji.be.routine.domain.Routine;
import com.haeyaji.be.routine.dto.RoutineApplyResponse;
import com.haeyaji.be.routine.dto.RoutineRequest;
import com.haeyaji.be.routine.dto.RoutineUpdateRequest;
import com.haeyaji.be.routine.repository.RoutineEntity;
import com.haeyaji.be.routine.repository.RoutineRepository;
import com.haeyaji.be.todo.domain.TodoSource;
import com.haeyaji.be.todo.repository.TodoEntity;
import com.haeyaji.be.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutineService {

    private final RoutineRepository routineRepository;
    private final TodoRepository todoRepository;

    @Transactional
    public Routine createRoutine(RoutineRequest request) {
        RoutineEntity entity = RoutineEntity.create(request.title(), request.startTime(), request.days());
        return routineRepository.save(entity).toDomain();
    }

    @Transactional
    public Routine updateRoutine(UUID id, RoutineUpdateRequest request) {
        if (request.title() != null && request.title().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
        if (request.days() != null && request.days().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
        RoutineEntity entity = routineRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        entity.update(request.title(), request.startTime(), request.days(), request.active());
        return entity.toDomain();
    }

    @Transactional
    public void deleteRoutine(UUID id) {
        RoutineEntity entity = routineRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        routineRepository.delete(entity);
    }

    /**
     * 활성 루틴을 지정 기간(from~to, 둘 다 포함)에 todo로 펼친다 (ROUT-4).
     * 같은 (날짜, 루틴) 조합으로 이미 만들어진 todo는 건너뛴다(ROUT-5).
     * 루틴을 나중에 수정해도 이미 생성된 todo는 소급 변경되지 않는다(FR-5.9) — 생성 시점 값을 그대로 복사해 넣기 때문.
     */
    @Transactional
    public RoutineApplyResponse applyRoutines(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
        List<RoutineEntity> activeRoutines = routineRepository.findByActiveTrue();
        int created = 0;
        for (RoutineEntity routine : activeRoutines) {
            created += applyRoutine(routine, from, to);
        }
        return new RoutineApplyResponse(created);
    }

    private int applyRoutine(RoutineEntity routine, LocalDate from, LocalDate to) {
        var days = routine.toDomain().days();
        int created = 0;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            if (!days.contains(date.getDayOfWeek())) {
                continue;
            }
            boolean exists = todoRepository.existsByTodoDateAndSourceAndSourceRefId(
                    date, TodoSource.ROUTINE, routine.getId());
            if (exists) {
                continue;
            }
            TodoEntity todo = TodoEntity.createFromRoutine(routine.getTitle(), date, routine.getStartTime(), routine.getId());
            todoRepository.save(todo);
            created++;
        }
        return created;
    }
}
