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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutineService {

    /** apply 기간 상한(일). 넓은 범위를 무제한 허용하면 활성 루틴 수만큼 곱해져 쿼리가 폭증할 수 있다(N3, DoS 방지). */
    private static final int MAX_APPLY_RANGE_DAYS = 90;

    private final RoutineRepository routineRepository;
    private final TodoRepository todoRepository;

    public List<Routine> getRoutines(UUID memberId) {
        return routineRepository.findAllByMemberId(memberId).stream()
                .map(RoutineEntity::toDomain)
                .toList();
    }

    public Routine getRoutine(UUID memberId, UUID id) {
        return routineRepository.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND))
                .toDomain();
    }

    @Transactional
    public Routine createRoutine(UUID memberId, RoutineRequest request) {
        RoutineEntity entity = RoutineEntity.create(
                memberId, request.title(), request.startTime(), request.labelId(), request.days());
        return routineRepository.save(entity).toDomain();
    }

    @Transactional
    public Routine updateRoutine(UUID memberId, UUID id, RoutineUpdateRequest request) {
        if (request.title() != null && request.title().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
        if (request.days() != null && request.days().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
        RoutineEntity entity = routineRepository.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        entity.update(request.title(), request.startTime(), request.days(), request.active(), request.labelId());
        return entity.toDomain();
    }

    @Transactional
    public void deleteRoutine(UUID memberId, UUID id) {
        RoutineEntity entity = routineRepository.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        routineRepository.delete(entity);
    }

    /**
     * 호출한 사용자 본인의 활성 루틴만 지정 기간(from~to, 둘 다 포함)에 todo로 펼친다 (ROUT-4).
     * 수동 트리거(POST /routines/apply)용 — 다른 회원의 루틴은 대상에 포함되지 않는다.
     * 같은 (날짜, 루틴) 조합으로 이미 만들어진 todo는 건너뛴다(ROUT-5).
     * 루틴을 나중에 수정해도 이미 생성된 todo는 소급 변경되지 않는다(FR-5.9) — 생성 시점 값을 그대로 복사해 넣기 때문.
     */
    @Transactional
    public RoutineApplyResponse applyRoutines(UUID memberId, LocalDate from, LocalDate to) {
        validateRange(from, to);
        List<RoutineEntity> activeRoutines = routineRepository.findByActiveTrueAndMemberId(memberId);
        int created = 0;
        for (RoutineEntity routine : activeRoutines) {
            created += applyRoutine(routine, from, to);
        }
        return new RoutineApplyResponse(created);
    }

    /**
     * 전 회원의 활성 루틴을 지정 기간에 todo로 펼친다. 스케줄러(자정 배치) 전용 —
     * 사용자 요청 경로(위 applyRoutines)와 달리 회원 범위를 의도적으로 제한하지 않는다.
     */
    @Transactional
    public RoutineApplyResponse applyRoutinesForAllMembers(LocalDate from, LocalDate to) {
        validateRange(from, to);
        List<RoutineEntity> activeRoutines = routineRepository.findByActiveTrue();
        int created = 0;
        for (RoutineEntity routine : activeRoutines) {
            created += applyRoutine(routine, from, to);
        }
        return new RoutineApplyResponse(created);
    }

    /**
     * from/to 순서와 최대 기간을 검증한다. 상한이 없으면 활성 루틴 수 × 기간 일수만큼 쿼리가
     * 곱연산으로 늘어나 넓은 범위(예: 2000~2100) 요청 하나로 DoS가 가능했다(N3).
     */
    private void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_APPLY_RANGE_DAYS) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
    }

    /**
     * exists 체크와 save 사이에 동시 요청(수동 apply와 자정 스케줄러가 겹치는 경우 등)이 끼어들면
     * 둘 다 exists=false를 보고 중복 todo를 만들 수 있었다(H6, TOCTOU). DB에 유니크 제약
     * (uk_todo_routine_dedup)을 추가해뒀으니, save 시점에 그 레이스가 실제로 발생하면
     * DataIntegrityViolationException으로 걸러지고 이 건만 건너뛴다 — exists 사전 체크는
     * 정상 경로에서 불필요한 제약 위반 예외를 피하기 위한 최적화로 그대로 둔다.
     */
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
            TodoEntity todo = TodoEntity.createFromRoutine(
                    routine.getMemberId(), routine.getTitle(), date, routine.getStartTime(), routine.getId());
            try {
                todoRepository.saveAndFlush(todo);
                created++;
            } catch (DataIntegrityViolationException e) {
                // 동시 요청이 먼저 같은 (date, ROUTINE, routineId) todo를 만든 경우 — 건너뛴다.
            }
        }
        return created;
    }
}
