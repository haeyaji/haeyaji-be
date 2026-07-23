package com.haeyaji.be.label.controller;

import com.haeyaji.be.common.response.ApiResponse;
import com.haeyaji.be.label.domain.Label;
import com.haeyaji.be.label.dto.LabelRequest;
import com.haeyaji.be.label.dto.LabelResponse;
import com.haeyaji.be.label.dto.LabelUpdateRequest;
import com.haeyaji.be.label.service.LabelService;
import com.haeyaji.be.member.domain.MemberRole;
import com.haeyaji.be.member.oauth.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * service 스텁으로 매핑·ApiResponse 래핑만 확인(프레임워크 바인딩은 제외).
 */
class LabelControllerTest {

    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final CustomUserDetails USER = new CustomUserDetails(MEMBER_ID, MemberRole.ROLE_USER);

    private Label label(String name, String color) {
        return Label.builder()
                .id(UUID.randomUUID())
                .name(name)
                .color(color)
                .build();
    }

    @Test
    void 목록조회는_본인_라벨을_반환한다() {
        LabelService service = mock(LabelService.class);
        when(service.getLabels(MEMBER_ID)).thenReturn(List.of(
                label("업무", "#FF0000"),
                label("운동", null)
        ));
        LabelController controller = new LabelController(service);

        ApiResponse<List<LabelResponse>> response = controller.getLabels(USER);

        assertThat(response.success()).isTrue();
        assertThat(response.data()).hasSize(2);
        assertThat(response.data()).extracting(LabelResponse::name)
                .containsExactlyInAnyOrder("업무", "운동");
    }

    @Test
    void 단건조회는_해당_라벨을_반환한다() {
        LabelService service = mock(LabelService.class);
        UUID id = UUID.randomUUID();
        when(service.getLabel(MEMBER_ID, id)).thenReturn(label("업무", "#FF0000"));
        LabelController controller = new LabelController(service);

        ApiResponse<LabelResponse> response = controller.getLabel(USER, id);

        assertThat(response.success()).isTrue();
        assertThat(response.data().name()).isEqualTo("업무");
    }

    @Test
    void 등록은_생성된_라벨을_담아_반환한다() {
        LabelService service = mock(LabelService.class);
        LabelRequest request = new LabelRequest("업무", "#FF0000");
        when(service.createLabel(MEMBER_ID, request)).thenReturn(label("업무", "#FF0000"));
        LabelController controller = new LabelController(service);

        ApiResponse<LabelResponse> response = controller.createLabel(USER, request);

        assertThat(response.success()).isTrue();
        assertThat(response.data().name()).isEqualTo("업무");
        assertThat(response.data().color()).isEqualTo("#FF0000");
    }

    @Test
    void 등록은_201로_매핑된다() throws NoSuchMethodException {
        ResponseStatus status = LabelController.class
                .getMethod("createLabel", CustomUserDetails.class, LabelRequest.class)
                .getAnnotation(ResponseStatus.class);

        assertThat(status.value()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void 수정은_수정된_라벨을_담아_반환한다() {
        LabelService service = mock(LabelService.class);
        UUID id = UUID.randomUUID();
        LabelUpdateRequest request = new LabelUpdateRequest(null, "#FFFFFF");
        when(service.updateLabel(MEMBER_ID, id, request)).thenReturn(label("업무", "#FFFFFF"));
        LabelController controller = new LabelController(service);

        ApiResponse<LabelResponse> response = controller.updateLabel(USER, id, request);

        assertThat(response.success()).isTrue();
        assertThat(response.data().color()).isEqualTo("#FFFFFF");
    }

    @Test
    void 삭제는_서비스에_위임하고_데이터없이_반환한다() {
        LabelService service = mock(LabelService.class);
        UUID id = UUID.randomUUID();
        LabelController controller = new LabelController(service);

        ApiResponse<Void> response = controller.deleteLabel(USER, id);

        verify(service).deleteLabel(MEMBER_ID, id);
        assertThat(response.success()).isTrue();
        assertThat(response.data()).isNull();
    }
}
