package com.haeyaji.be.routine.service;

import com.haeyaji.be.common.exception.BusinessException;
import com.haeyaji.be.common.exception.ErrorCode;
import com.haeyaji.be.routine.domain.Routine;
import com.haeyaji.be.routine.dto.RoutineRequest;
import com.haeyaji.be.routine.dto.RoutineUpdateRequest;
import com.haeyaji.be.routine.repository.RoutineEntity;
import com.haeyaji.be.routine.repository.RoutineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutineService {

    private final RoutineRepository routineRepository;

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
}
