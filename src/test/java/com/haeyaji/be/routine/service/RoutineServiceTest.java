package com.haeyaji.be.routine.service;

import com.haeyaji.be.routine.domain.Routine;
import com.haeyaji.be.routine.dto.RoutineRequest;
import com.haeyaji.be.routine.repository.RoutineEntity;
import com.haeyaji.be.routine.repository.RoutineRepository;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoutineServiceTest {

    @Test
    void 등록하면_활성상태로_생성된다() {
        RoutineRepository repo = mock(RoutineRepository.class);
        when(repo.save(any(RoutineEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        RoutineService service = new RoutineService(repo);

        Routine routine = service.createRoutine(
                new RoutineRequest("아침 운동", LocalTime.of(7, 0), Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)));

        assertThat(routine.title()).isEqualTo("아침 운동");
        assertThat(routine.startTime()).isEqualTo(LocalTime.of(7, 0));
        assertThat(routine.active()).isTrue();
        assertThat(routine.days()).containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
    }

    @Test
    void 등록시_시간_미지정이면_null로_저장된다() {
        RoutineRepository repo = mock(RoutineRepository.class);
        when(repo.save(any(RoutineEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        RoutineService service = new RoutineService(repo);

        Routine routine = service.createRoutine(
                new RoutineRequest("독서", null, Set.of(DayOfWeek.SUNDAY)));

        assertThat(routine.startTime()).isNull();
    }
}
