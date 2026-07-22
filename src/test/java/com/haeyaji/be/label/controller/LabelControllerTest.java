package com.haeyaji.be.label.controller;

import com.haeyaji.be.common.response.ApiResponse;
import com.haeyaji.be.label.domain.Label;
import com.haeyaji.be.label.dto.LabelRequest;
import com.haeyaji.be.label.dto.LabelResponse;
import com.haeyaji.be.label.service.LabelService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * service 스텁으로 매핑·ApiResponse 래핑만 확인(프레임워크 바인딩은 제외).
 */
class LabelControllerTest {

    private Label label(String name, String color) {
        return Label.builder()
                .id(UUID.randomUUID())
                .name(name)
                .color(color)
                .build();
    }

    @Test
    void 등록은_생성된_라벨을_담아_반환한다() {
        LabelService service = mock(LabelService.class);
        LabelRequest request = new LabelRequest("업무", "#FF0000");
        when(service.createLabel(request)).thenReturn(label("업무", "#FF0000"));
        LabelController controller = new LabelController(service);

        ApiResponse<LabelResponse> response = controller.createLabel(request);

        assertThat(response.success()).isTrue();
        assertThat(response.data().name()).isEqualTo("업무");
        assertThat(response.data().color()).isEqualTo("#FF0000");
    }

    @Test
    void 등록은_201로_매핑된다() throws NoSuchMethodException {
        ResponseStatus status = LabelController.class
                .getMethod("createLabel", LabelRequest.class)
                .getAnnotation(ResponseStatus.class);

        assertThat(status.value()).isEqualTo(HttpStatus.CREATED);
    }
}
