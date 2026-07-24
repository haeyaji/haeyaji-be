package com.haeyaji.be.routine.service;

import com.haeyaji.be.common.exception.BusinessException;
import com.haeyaji.be.common.exception.ErrorCode;
import com.haeyaji.be.routine.domain.DayPreset;
import com.haeyaji.be.routine.domain.Routine;
import com.haeyaji.be.routine.dto.RoutineApplyResponse;
import com.haeyaji.be.routine.dto.RoutineRequest;
import com.haeyaji.be.routine.dto.RoutineUpdateRequest;
import com.haeyaji.be.routine.repository.RoutineEntity;
import com.haeyaji.be.routine.repository.RoutineRepository;
import com.haeyaji.be.todo.domain.TodoSource;
import com.haeyaji.be.todo.repository.TodoEntity;
import com.haeyaji.be.todo.repository.TodoRepository;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoutineServiceTest {

    private static final UUID MEMBER_ID = UUID.randomUUID();

    @Test
    void 목록조회는_본인_루틴만_반환한다() {
        RoutineRepository repo = mock(RoutineRepository.class);
        RoutineEntity a = RoutineEntity.create(MEMBER_ID, "루틴A", null, null, Set.of(DayOfWeek.MONDAY));
        RoutineEntity b = RoutineEntity.create(MEMBER_ID, "루틴B", null, null, Set.of(DayOfWeek.SUNDAY));
        when(repo.findAllByMemberId(MEMBER_ID)).thenReturn(List.of(a, b));
        RoutineService service = new RoutineService(repo, mock(TodoRepository.class), mock(RoutineTodoWriter.class));

        List<Routine> routines = service.getRoutines(MEMBER_ID);

        assertThat(routines).hasSize(2);
        assertThat(routines).extracting(Routine::title).containsExactlyInAnyOrder("루틴A", "루틴B");
    }

    @Test
    void 단건조회는_존재하지_않으면_예외() {
        RoutineRepository repo = mock(RoutineRepository.class);
        UUID id = UUID.randomUUID();
        when(repo.findByIdAndMemberId(id, MEMBER_ID)).thenReturn(Optional.empty());
        RoutineService service = new RoutineService(repo, mock(TodoRepository.class), mock(RoutineTodoWriter.class));

        assertThatThrownBy(() -> service.getRoutine(MEMBER_ID, id))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void 다른_회원의_루틴을_조회하려하면_예외() {
        RoutineRepository repo = mock(RoutineRepository.class);
        UUID id = UUID.randomUUID();
        UUID otherMemberId = UUID.randomUUID();
        when(repo.findByIdAndMemberId(id, otherMemberId)).thenReturn(Optional.empty());
        RoutineService service = new RoutineService(repo, mock(TodoRepository.class), mock(RoutineTodoWriter.class));

        assertThatThrownBy(() -> service.getRoutine(otherMemberId, id))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void 단건조회는_존재하면_반환한다() {
        RoutineRepository repo = mock(RoutineRepository.class);
        UUID id = UUID.randomUUID();
        RoutineEntity existing = RoutineEntity.create(MEMBER_ID, "루틴", null, null, Set.of(DayOfWeek.MONDAY));
        when(repo.findByIdAndMemberId(id, MEMBER_ID)).thenReturn(Optional.of(existing));
        RoutineService service = new RoutineService(repo, mock(TodoRepository.class), mock(RoutineTodoWriter.class));

        Routine routine = service.getRoutine(MEMBER_ID, id);

        assertThat(routine.title()).isEqualTo("루틴");
    }

    @Test
    void 등록하면_활성상태로_본인_소유로_생성된다() {
        RoutineRepository repo = mock(RoutineRepository.class);
        when(repo.save(any(RoutineEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        RoutineService service = new RoutineService(repo, mock(TodoRepository.class), mock(RoutineTodoWriter.class));

        Routine routine = service.createRoutine(MEMBER_ID,
                new RoutineRequest("아침 운동", LocalTime.of(7, 0), Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), null));

        assertThat(routine.memberId()).isEqualTo(MEMBER_ID);
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
        RoutineService service = new RoutineService(repo, mock(TodoRepository.class), mock(RoutineTodoWriter.class));

        Routine routine = service.createRoutine(MEMBER_ID, new RoutineRequest("출근", null,
                Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), null));

        assertThat(routine.preset()).isEqualTo(DayPreset.WEEKDAY);
    }

    @Test
    void 등록시_라벨을_지정하면_반영된다() {
        RoutineRepository repo = mock(RoutineRepository.class);
        when(repo.save(any(RoutineEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        RoutineService service = new RoutineService(repo, mock(TodoRepository.class), mock(RoutineTodoWriter.class));
        UUID labelId = UUID.randomUUID();

        Routine routine = service.createRoutine(MEMBER_ID,
                new RoutineRequest("카페 투어", null, Set.of(DayOfWeek.SATURDAY), labelId));

        assertThat(routine.labelId()).isEqualTo(labelId);
    }

    @Test
    void 등록시_시간_미지정이면_null로_저장된다() {
        RoutineRepository repo = mock(RoutineRepository.class);
        when(repo.save(any(RoutineEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        RoutineService service = new RoutineService(repo, mock(TodoRepository.class), mock(RoutineTodoWriter.class));

        Routine routine = service.createRoutine(MEMBER_ID,
                new RoutineRequest("독서", null, Set.of(DayOfWeek.SUNDAY), null));

        assertThat(routine.startTime()).isNull();
    }

    private RoutineEntity entity(String title, LocalTime startTime, Set<DayOfWeek> days) {
        return RoutineEntity.create(MEMBER_ID, title, startTime, null, days);
    }

    private RoutineUpdateRequest updateRequest(String title, LocalTime startTime, Set<DayOfWeek> days, Boolean active) {
        return new RoutineUpdateRequest(title, startTime, days, active, null);
    }

    @Test
    void 수정시_존재하지_않으면_예외() {
        RoutineRepository repo = mock(RoutineRepository.class);
        UUID id = UUID.randomUUID();
        when(repo.findByIdAndMemberId(id, MEMBER_ID)).thenReturn(Optional.empty());
        RoutineService service = new RoutineService(repo, mock(TodoRepository.class), mock(RoutineTodoWriter.class));

        assertThatThrownBy(() -> service.updateRoutine(MEMBER_ID, id, updateRequest(null, null, null, true)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void 수정시_제목이_공백이면_예외() {
        RoutineRepository repo = mock(RoutineRepository.class);
        RoutineService service = new RoutineService(repo, mock(TodoRepository.class), mock(RoutineTodoWriter.class));

        assertThatThrownBy(() -> service.updateRoutine(MEMBER_ID, UUID.randomUUID(), updateRequest("   ", null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    @Test
    void 수정시_요일이_빈집합이면_예외() {
        RoutineRepository repo = mock(RoutineRepository.class);
        RoutineService service = new RoutineService(repo, mock(TodoRepository.class), mock(RoutineTodoWriter.class));

        assertThatThrownBy(() -> service.updateRoutine(MEMBER_ID, UUID.randomUUID(), updateRequest(null, null, Set.of(), null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    @Test
    void 부분수정시_보내지_않은_필드는_유지된다() {
        RoutineRepository repo = mock(RoutineRepository.class);
        UUID id = UUID.randomUUID();
        RoutineEntity existing = entity("기존 루틴", LocalTime.of(7, 0), Set.of(DayOfWeek.MONDAY));
        when(repo.findByIdAndMemberId(id, MEMBER_ID)).thenReturn(Optional.of(existing));
        RoutineService service = new RoutineService(repo, mock(TodoRepository.class), mock(RoutineTodoWriter.class));

        Routine routine = service.updateRoutine(MEMBER_ID, id, updateRequest(null, null, null, false));

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
        when(repo.findByIdAndMemberId(id, MEMBER_ID)).thenReturn(Optional.of(existing));
        RoutineService service = new RoutineService(repo, mock(TodoRepository.class), mock(RoutineTodoWriter.class));

        Routine routine = service.updateRoutine(MEMBER_ID, id,
                updateRequest(null, null, Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), null));

        assertThat(routine.days()).containsExactlyInAnyOrder(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        assertThat(routine.preset()).isEqualTo(DayPreset.WEEKEND);
    }

    @Test
    void 라벨을_지정하면_반영된다() {
        RoutineRepository repo = mock(RoutineRepository.class);
        UUID id = UUID.randomUUID();
        RoutineEntity existing = entity("루틴", null, Set.of(DayOfWeek.MONDAY));
        when(repo.findByIdAndMemberId(id, MEMBER_ID)).thenReturn(Optional.of(existing));
        RoutineService service = new RoutineService(repo, mock(TodoRepository.class), mock(RoutineTodoWriter.class));
        UUID labelId = UUID.randomUUID();

        Routine routine = service.updateRoutine(MEMBER_ID, id, new RoutineUpdateRequest(null, null, null, null, labelId));

        assertThat(routine.labelId()).isEqualTo(labelId);
    }

    @Test
    void 삭제시_존재하지_않으면_예외() {
        RoutineRepository repo = mock(RoutineRepository.class);
        UUID id = UUID.randomUUID();
        when(repo.findByIdAndMemberId(id, MEMBER_ID)).thenReturn(Optional.empty());
        RoutineService service = new RoutineService(repo, mock(TodoRepository.class), mock(RoutineTodoWriter.class));

        assertThatThrownBy(() -> service.deleteRoutine(MEMBER_ID, id))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void 삭제시_존재하면_리포지토리에서_삭제된다() {
        RoutineRepository repo = mock(RoutineRepository.class);
        UUID id = UUID.randomUUID();
        RoutineEntity existing = entity("루틴", null, Set.of(DayOfWeek.MONDAY));
        when(repo.findByIdAndMemberId(id, MEMBER_ID)).thenReturn(Optional.of(existing));
        RoutineService service = new RoutineService(repo, mock(TodoRepository.class), mock(RoutineTodoWriter.class));

        service.deleteRoutine(MEMBER_ID, id);

        verify(repo).delete(existing);
    }

    @Test
    void 일괄등록시_from이_to보다_늦으면_예외() {
        RoutineRepository repo = mock(RoutineRepository.class);
        RoutineService service = new RoutineService(repo, mock(TodoRepository.class), mock(RoutineTodoWriter.class));
        LocalDate from = LocalDate.of(2026, 8, 3);
        LocalDate to = LocalDate.of(2026, 7, 27);

        assertThatThrownBy(() -> service.applyRoutines(MEMBER_ID, from, to))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    @Test
    void 일괄등록시_기간이_90일을_넘으면_예외() {
        RoutineRepository repo = mock(RoutineRepository.class);
        RoutineService service = new RoutineService(repo, mock(TodoRepository.class), mock(RoutineTodoWriter.class));
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);

        assertThatThrownBy(() -> service.applyRoutines(MEMBER_ID, from, to))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    @Test
    void 일괄등록시_기간이_90일_이하면_허용된다() {
        RoutineRepository repo = mock(RoutineRepository.class);
        when(repo.findByActiveTrueAndMemberId(MEMBER_ID)).thenReturn(List.of());
        RoutineService service = new RoutineService(repo, mock(TodoRepository.class), mock(RoutineTodoWriter.class));
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = from.plusDays(90);

        RoutineApplyResponse result = service.applyRoutines(MEMBER_ID, from, to);

        assertThat(result.created()).isEqualTo(0);
    }

    @Test
    void 일괄등록시_반복요일에_해당하는_날짜만_todo로_생성된다() {
        RoutineRepository repo = mock(RoutineRepository.class);
        TodoRepository todoRepo = mock(TodoRepository.class);
        RoutineTodoWriter writer = mock(RoutineTodoWriter.class);
        RoutineEntity routine = entity("아침 운동", LocalTime.of(7, 0),
                Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY));
        when(repo.findByActiveTrueAndMemberId(MEMBER_ID)).thenReturn(List.of(routine));
        when(todoRepo.existsByTodoDateAndSourceAndSourceRefId(any(), eq(TodoSource.ROUTINE), eq(routine.getId())))
                .thenReturn(false);
        RoutineService service = new RoutineService(repo, todoRepo, writer);

        // 2026-07-27(월) ~ 2026-08-02(일), 1주
        RoutineApplyResponse result = service.applyRoutines(
                MEMBER_ID, LocalDate.of(2026, 7, 27), LocalDate.of(2026, 8, 2));

        assertThat(result.created()).isEqualTo(3);
        verify(writer, times(3)).save(any(TodoEntity.class));
    }

    @Test
    void 일괄등록시_동시요청으로_유니크제약이_걸리면_해당_건만_건너뛴다() {
        RoutineRepository repo = mock(RoutineRepository.class);
        TodoRepository todoRepo = mock(TodoRepository.class);
        RoutineTodoWriter writer = mock(RoutineTodoWriter.class);
        RoutineEntity routine = entity("독서", null, Set.of(DayOfWeek.SUNDAY));
        when(repo.findByActiveTrueAndMemberId(MEMBER_ID)).thenReturn(List.of(routine));
        when(todoRepo.existsByTodoDateAndSourceAndSourceRefId(any(), eq(TodoSource.ROUTINE), eq(routine.getId())))
                .thenReturn(false);
        org.mockito.Mockito.doThrow(new org.springframework.dao.DataIntegrityViolationException("uk_todo_routine_dedup"))
                .when(writer).save(any(TodoEntity.class));
        RoutineService service = new RoutineService(repo, todoRepo, writer);

        RoutineApplyResponse result = service.applyRoutines(
                MEMBER_ID, LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 2));

        assertThat(result.created()).isEqualTo(0);
    }

    @Test
    void 일괄등록시_이미_생성된_todo는_건너뛴다() {
        RoutineRepository repo = mock(RoutineRepository.class);
        TodoRepository todoRepo = mock(TodoRepository.class);
        RoutineTodoWriter writer = mock(RoutineTodoWriter.class);
        RoutineEntity routine = entity("독서", null, Set.of(DayOfWeek.SUNDAY));
        when(repo.findByActiveTrueAndMemberId(MEMBER_ID)).thenReturn(List.of(routine));
        when(todoRepo.existsByTodoDateAndSourceAndSourceRefId(any(), eq(TodoSource.ROUTINE), eq(routine.getId())))
                .thenReturn(true);
        RoutineService service = new RoutineService(repo, todoRepo, writer);

        RoutineApplyResponse result = service.applyRoutines(
                MEMBER_ID, LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 2));

        assertThat(result.created()).isEqualTo(0);
        verify(writer, never()).save(any(TodoEntity.class));
    }

    @Test
    void 일괄등록시_비활성_루틴은_대상에서_제외된다() {
        RoutineRepository repo = mock(RoutineRepository.class);
        TodoRepository todoRepo = mock(TodoRepository.class);
        RoutineTodoWriter writer = mock(RoutineTodoWriter.class);
        when(repo.findByActiveTrueAndMemberId(MEMBER_ID)).thenReturn(List.of());
        RoutineService service = new RoutineService(repo, todoRepo, writer);

        RoutineApplyResponse result = service.applyRoutines(
                MEMBER_ID, LocalDate.of(2026, 7, 27), LocalDate.of(2026, 8, 2));

        assertThat(result.created()).isEqualTo(0);
        verify(writer, never()).save(any(TodoEntity.class));
    }

    @Test
    void 일괄등록시_다른_회원의_루틴은_대상에서_제외된다() {
        RoutineRepository repo = mock(RoutineRepository.class);
        TodoRepository todoRepo = mock(TodoRepository.class);
        RoutineTodoWriter writer = mock(RoutineTodoWriter.class);
        when(repo.findByActiveTrueAndMemberId(MEMBER_ID)).thenReturn(List.of());
        RoutineService service = new RoutineService(repo, todoRepo, writer);

        RoutineApplyResponse result = service.applyRoutines(
                MEMBER_ID, LocalDate.of(2026, 7, 27), LocalDate.of(2026, 8, 2));

        assertThat(result.created()).isEqualTo(0);
        verify(repo, never()).findByActiveTrue();
        verify(writer, never()).save(any(TodoEntity.class));
    }

    @Test
    void 전회원_일괄등록시_회원_구분없이_전체_활성루틴이_대상이_된다() {
        RoutineRepository repo = mock(RoutineRepository.class);
        TodoRepository todoRepo = mock(TodoRepository.class);
        RoutineTodoWriter writer = mock(RoutineTodoWriter.class);
        RoutineEntity routineOfMemberA = entity("루틴A", null, Set.of(DayOfWeek.SUNDAY));
        when(repo.findByActiveTrue()).thenReturn(List.of(routineOfMemberA));
        when(todoRepo.existsByTodoDateAndSourceAndSourceRefId(any(), eq(TodoSource.ROUTINE), eq(routineOfMemberA.getId())))
                .thenReturn(false);
        RoutineService service = new RoutineService(repo, todoRepo, writer);

        RoutineApplyResponse result = service.applyRoutinesForAllMembers(
                LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 2));

        assertThat(result.created()).isEqualTo(1);
        verify(repo, never()).findByActiveTrueAndMemberId(any());
    }
}
