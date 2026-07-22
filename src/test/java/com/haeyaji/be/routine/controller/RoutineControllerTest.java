package com.haeyaji.be.routine.controller;

import com.haeyaji.be.common.response.ApiResponse;
import com.haeyaji.be.routine.domain.DayPreset;
import com.haeyaji.be.routine.domain.Routine;
import com.haeyaji.be.routine.dto.RoutineApplyRequest;
import com.haeyaji.be.routine.dto.RoutineApplyResponse;
import com.haeyaji.be.routine.dto.RoutineRequest;
import com.haeyaji.be.routine.dto.RoutineResponse;
import com.haeyaji.be.routine.dto.RoutineUpdateRequest;
import com.haeyaji.be.routine.service.RoutineService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * service 스텁으로 매핑·ApiResponse 래핑만 확인(프레임워크 바인딩은 제외).
 */
class RoutineControllerTest {

    private Routine routine(String title, Set<DayOfWeek> days) {
        return Routine.builder()
                .id(UUID.randomUUID())
                .title(title)
                .startTime(LocalTime.of(7, 0))
                .active(true)
                .days(days)
                .build();
    }

    @Test
    void 목록조회는_전체_루틴을_반환한다() {
        RoutineService service = mock(RoutineService.class);
        when(service.getRoutines()).thenReturn(List.of(
                routine("루틴A", Set.of(DayOfWeek.MONDAY)),
                routine("루틴B", Set.of(DayOfWeek.SUNDAY))
        ));
        RoutineController controller = new RoutineController(service);

        ApiResponse<List<RoutineResponse>> response = controller.getRoutines();

        assertThat(response.success()).isTrue();
        assertThat(response.data()).hasSize(2);
        assertThat(response.data()).extracting(RoutineResponse::title)
                .containsExactlyInAnyOrder("루틴A", "루틴B");
    }

    @Test
    void 단건조회는_해당_루틴을_반환한다() {
        RoutineService service = mock(RoutineService.class);
        UUID id = UUID.randomUUID();
        when(service.getRoutine(id)).thenReturn(routine("아침 운동", Set.of(DayOfWeek.MONDAY)));
        RoutineController controller = new RoutineController(service);

        ApiResponse<RoutineResponse> response = controller.getRoutine(id);

        assertThat(response.success()).isTrue();
        assertThat(response.data().title()).isEqualTo("아침 운동");
    }

    @Test
    void 등록은_생성된_루틴을_담아_반환한다() {
        RoutineService service = mock(RoutineService.class);
        RoutineRequest request = new RoutineRequest("아침 운동", LocalTime.of(7, 0), Set.of(DayOfWeek.MONDAY), null);
        when(service.createRoutine(request)).thenReturn(routine("아침 운동", Set.of(DayOfWeek.MONDAY)));
        RoutineController controller = new RoutineController(service);

        ApiResponse<RoutineResponse> response = controller.createRoutine(request);

        assertThat(response.success()).isTrue();
        assertThat(response.data().title()).isEqualTo("아침 운동");
        assertThat(response.data().days()).containsExactly(DayOfWeek.MONDAY);
        assertThat(response.data().preset()).isEqualTo(DayPreset.CUSTOM);
    }

    @Test
    void 등록은_201로_매핑된다() throws NoSuchMethodException {
        ResponseStatus status = RoutineController.class
                .getMethod("createRoutine", RoutineRequest.class)
                .getAnnotation(ResponseStatus.class);

        assertThat(status.value()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void 수정은_수정된_루틴을_담아_반환한다() {
        RoutineService service = mock(RoutineService.class);
        UUID id = UUID.randomUUID();
        RoutineUpdateRequest request = new RoutineUpdateRequest(null, null, null, false, null);
        when(service.updateRoutine(id, request)).thenReturn(routine("아침 운동", Set.of(DayOfWeek.MONDAY)));
        RoutineController controller = new RoutineController(service);

        ApiResponse<RoutineResponse> response = controller.updateRoutine(id, request);

        assertThat(response.success()).isTrue();
        assertThat(response.data().title()).isEqualTo("아침 운동");
    }

    @Test
    void 삭제는_서비스에_위임하고_데이터없이_반환한다() {
        RoutineService service = mock(RoutineService.class);
        UUID id = UUID.randomUUID();
        RoutineController controller = new RoutineController(service);

        ApiResponse<Void> response = controller.deleteRoutine(id);

        verify(service).deleteRoutine(id);
        assertThat(response.success()).isTrue();
        assertThat(response.data()).isNull();
    }

    @Test
    void 일괄등록은_생성_건수를_담아_반환한다() {
        RoutineService service = mock(RoutineService.class);
        RoutineApplyRequest request = new RoutineApplyRequest(
                LocalDate.of(2026, 7, 27), LocalDate.of(2026, 8, 2));
        when(service.applyRoutines(request.from(), request.to())).thenReturn(new RoutineApplyResponse(3));
        RoutineController controller = new RoutineController(service);

        ApiResponse<RoutineApplyResponse> response = controller.applyRoutines(request);

        assertThat(response.success()).isTrue();
        assertThat(response.data().created()).isEqualTo(3);
    }
}
