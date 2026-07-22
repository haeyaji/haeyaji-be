package com.haeyaji.be.routine.service;

import com.haeyaji.be.common.exception.BusinessException;
import com.haeyaji.be.common.exception.ErrorCode;
import com.haeyaji.be.routine.domain.DayPreset;
import com.haeyaji.be.routine.domain.Routine;
import com.haeyaji.be.routine.dto.RoutineRequest;
import com.haeyaji.be.routine.dto.RoutineUpdateRequest;
import com.haeyaji.be.routine.repository.RoutineEntity;
import com.haeyaji.be.routine.repository.RoutineRepository;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
        assertThat(routine.preset()).isEqualTo(DayPreset.CUSTOM);
    }

    @Test
    void 요일이_평일이면_프리셋이_평일로_판정된다() {
        RoutineRepository repo = mock(RoutineRepository.class);
        when(repo.save(any(RoutineEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        RoutineService service = new RoutineService(repo);

        Routine routine = service.createRoutine(new RoutineRequest("출근", null,
                Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)));

        assertThat(routine.preset()).isEqualTo(DayPreset.WEEKDAY);
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

    private RoutineEntity entity(String title, LocalTime startTime, Set<DayOfWeek> days) {
        return RoutineEntity.create(title, startTime, days);
    }

    private RoutineUpdateRequest updateRequest(String title, LocalTime startTime, Set<DayOfWeek> days, Boolean active) {
        return new RoutineUpdateRequest(title, startTime, days, active);
    }

    @Test
    void 수정시_존재하지_않으면_예외() {
        RoutineRepository repo = mock(RoutineRepository.class);
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());
        RoutineService service = new RoutineService(repo);

        assertThatThrownBy(() -> service.updateRoutine(id, updateRequest(null, null, null, true)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void 수정시_제목이_공백이면_예외() {
        RoutineRepository repo = mock(RoutineRepository.class);
        RoutineService service = new RoutineService(repo);

        assertThatThrownBy(() -> service.updateRoutine(UUID.randomUUID(), updateRequest("   ", null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    @Test
    void 수정시_요일이_빈집합이면_예외() {
        RoutineRepository repo = mock(RoutineRepository.class);
        RoutineService service = new RoutineService(repo);

        assertThatThrownBy(() -> service.updateRoutine(UUID.randomUUID(), updateRequest(null, null, Set.of(), null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    @Test
    void 부분수정시_보내지_않은_필드는_유지된다() {
        RoutineRepository repo = mock(RoutineRepository.class);
        UUID id = UUID.randomUUID();
        RoutineEntity existing = entity("기존 루틴", LocalTime.of(7, 0), Set.of(DayOfWeek.MONDAY));
        when(repo.findById(id)).thenReturn(Optional.of(existing));
        RoutineService service = new RoutineService(repo);

        Routine routine = service.updateRoutine(id, updateRequest(null, null, null, false));

        assertThat(routine.title()).isEqualTo("기존 루틴");
        assertThat(routine.startTime()).isEqualTo(LocalTime.of(7, 0));
        assertThat(routine.days()).containsExactly(DayOfWeek.MONDAY);
        assertThat(routine.active()).isFalse();
    }

    @Test
    void 요일을_수정하면_기존_요일이_전부_교체된다() {
        RoutineRepository repo = mock(RoutineRepository.class);
        UUID id = UUID.randomUUID();
        RoutineEntity existing = entity("루틴", null, Set.of(DayOfWeek.MONDAY));
        when(repo.findById(id)).thenReturn(Optional.of(existing));
        RoutineService service = new RoutineService(repo);

        Routine routine = service.updateRoutine(id,
                updateRequest(null, null, Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), null));

        assertThat(routine.days()).containsExactlyInAnyOrder(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        assertThat(routine.preset()).isEqualTo(DayPreset.WEEKEND);
    }

    @Test
    void 삭제시_존재하지_않으면_예외() {
        RoutineRepository repo = mock(RoutineRepository.class);
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());
        RoutineService service = new RoutineService(repo);

        assertThatThrownBy(() -> service.deleteRoutine(id))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void 삭제시_존재하면_리포지토리에서_삭제된다() {
        RoutineRepository repo = mock(RoutineRepository.class);
        UUID id = UUID.randomUUID();
        RoutineEntity existing = entity("루틴", null, Set.of(DayOfWeek.MONDAY));
        when(repo.findById(id)).thenReturn(Optional.of(existing));
        RoutineService service = new RoutineService(repo);

        service.deleteRoutine(id);

        verify(repo).delete(existing);
    }
}
