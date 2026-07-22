package com.haeyaji.be.routine.service;

import com.haeyaji.be.routine.domain.Routine;
import com.haeyaji.be.routine.dto.RoutineRequest;
import com.haeyaji.be.routine.repository.RoutineEntity;
import com.haeyaji.be.routine.repository.RoutineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
