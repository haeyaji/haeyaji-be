package com.haeyaji.be.label.service;

import com.haeyaji.be.label.domain.Label;
import com.haeyaji.be.label.dto.LabelRequest;
import com.haeyaji.be.label.repository.LabelEntity;
import com.haeyaji.be.label.repository.LabelRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LabelServiceTest {

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
}
