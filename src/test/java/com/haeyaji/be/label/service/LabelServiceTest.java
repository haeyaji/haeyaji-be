package com.haeyaji.be.label.service;

import com.haeyaji.be.common.exception.BusinessException;
import com.haeyaji.be.common.exception.ErrorCode;
import com.haeyaji.be.label.domain.Label;
import com.haeyaji.be.label.dto.LabelRequest;
import com.haeyaji.be.label.dto.LabelUpdateRequest;
import com.haeyaji.be.label.repository.LabelEntity;
import com.haeyaji.be.label.repository.LabelRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LabelServiceTest {

    @Test
    void 목록조회는_전체_라벨을_반환한다() {
        LabelRepository repo = mock(LabelRepository.class);
        LabelEntity a = LabelEntity.create(null, "업무", "#FF0000");
        LabelEntity b = LabelEntity.create(null, "운동", null);
        when(repo.findAll()).thenReturn(List.of(a, b));
        LabelService service = new LabelService(repo);

        List<Label> labels = service.getLabels();

        assertThat(labels).hasSize(2);
        assertThat(labels).extracting(Label::name).containsExactlyInAnyOrder("업무", "운동");
    }

    @Test
    void 단건조회는_존재하지_않으면_예외() {
        LabelRepository repo = mock(LabelRepository.class);
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());
        LabelService service = new LabelService(repo);

        assertThatThrownBy(() -> service.getLabel(id))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void 단건조회는_존재하면_반환한다() {
        LabelRepository repo = mock(LabelRepository.class);
        UUID id = UUID.randomUUID();
        LabelEntity existing = LabelEntity.create(null, "업무", "#FF0000");
        when(repo.findById(id)).thenReturn(Optional.of(existing));
        LabelService service = new LabelService(repo);

        Label label = service.getLabel(id);

        assertThat(label.name()).isEqualTo("업무");
    }

    @Test
    void 등록하면_이름과_색상이_반영된다() {
        LabelRepository repo = mock(LabelRepository.class);
        when(repo.save(any(LabelEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        LabelService service = new LabelService(repo);

        Label label = service.createLabel(new LabelRequest("업무", "#FF0000"));

        assertThat(label.name()).isEqualTo("업무");
        assertThat(label.color()).isEqualTo("#FF0000");
    }

    @Test
    void 등록시_색상_미지정이면_null로_저장된다() {
        LabelRepository repo = mock(LabelRepository.class);
        when(repo.save(any(LabelEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        LabelService service = new LabelService(repo);

        Label label = service.createLabel(new LabelRequest("운동", null));

        assertThat(label.color()).isNull();
    }

    @Test
    void 수정시_존재하지_않으면_예외() {
        LabelRepository repo = mock(LabelRepository.class);
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());
        LabelService service = new LabelService(repo);

        assertThatThrownBy(() -> service.updateLabel(id, new LabelUpdateRequest("새이름", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void 수정시_이름이_공백이면_예외() {
        LabelRepository repo = mock(LabelRepository.class);
        LabelService service = new LabelService(repo);

        assertThatThrownBy(() -> service.updateLabel(UUID.randomUUID(), new LabelUpdateRequest("   ", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    @Test
    void 부분수정시_보내지_않은_필드는_유지된다() {
        LabelRepository repo = mock(LabelRepository.class);
        UUID id = UUID.randomUUID();
        LabelEntity existing = LabelEntity.create(null, "기존이름", "#000000");
        when(repo.findById(id)).thenReturn(Optional.of(existing));
        LabelService service = new LabelService(repo);

        Label label = service.updateLabel(id, new LabelUpdateRequest(null, "#FFFFFF"));

        assertThat(label.name()).isEqualTo("기존이름");
        assertThat(label.color()).isEqualTo("#FFFFFF");
    }

    @Test
    void 삭제시_존재하지_않으면_예외() {
        LabelRepository repo = mock(LabelRepository.class);
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());
        LabelService service = new LabelService(repo);

        assertThatThrownBy(() -> service.deleteLabel(id))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void 삭제시_존재하면_리포지토리에서_삭제된다() {
        LabelRepository repo = mock(LabelRepository.class);
        UUID id = UUID.randomUUID();
        LabelEntity existing = LabelEntity.create(null, "업무", null);
        when(repo.findById(id)).thenReturn(Optional.of(existing));
        LabelService service = new LabelService(repo);

        service.deleteLabel(id);

        verify(repo).delete(existing);
    }
}
