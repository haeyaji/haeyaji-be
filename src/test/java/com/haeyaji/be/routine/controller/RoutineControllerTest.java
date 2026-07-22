package com.haeyaji.be.routine.controller;

import com.haeyaji.be.common.response.ApiResponse;
import com.haeyaji.be.routine.domain.Routine;
import com.haeyaji.be.routine.dto.RoutineRequest;
import com.haeyaji.be.routine.dto.RoutineResponse;
import com.haeyaji.be.routine.service.RoutineService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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
    void 등록은_생성된_루틴을_담아_반환한다() {
        RoutineService service = mock(RoutineService.class);
        RoutineRequest request = new RoutineRequest("아침 운동", LocalTime.of(7, 0), Set.of(DayOfWeek.MONDAY));
        when(service.createRoutine(request)).thenReturn(routine("아침 운동", Set.of(DayOfWeek.MONDAY)));
        RoutineController controller = new RoutineController(service);

        ApiResponse<RoutineResponse> response = controller.createRoutine(request);

        assertThat(response.success()).isTrue();
        assertThat(response.data().title()).isEqualTo("아침 운동");
        assertThat(response.data().days()).containsExactly(DayOfWeek.MONDAY);
    }

    @Test
    void 등록은_201로_매핑된다() throws NoSuchMethodException {
        ResponseStatus status = RoutineController.class
                .getMethod("createRoutine", RoutineRequest.class)
                .getAnnotation(ResponseStatus.class);

        assertThat(status.value()).isEqualTo(HttpStatus.CREATED);
    }
}
