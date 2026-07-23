package com.haeyaji.be.label.service;

import com.haeyaji.be.common.exception.BusinessException;
import com.haeyaji.be.common.exception.ErrorCode;
import com.haeyaji.be.label.domain.Label;
import com.haeyaji.be.label.dto.LabelRequest;
import com.haeyaji.be.label.dto.LabelUpdateRequest;
import com.haeyaji.be.label.repository.LabelEntity;
import com.haeyaji.be.label.repository.LabelRepository;
import com.haeyaji.be.routine.repository.RoutineRepository;
import com.haeyaji.be.todo.repository.TodoRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LabelServiceTest {

    private static final UUID MEMBER_ID = UUID.randomUUID();

    private LabelService service(LabelRepository repo) {
        return service(repo, mock(TodoRepository.class), mock(RoutineRepository.class));
    }

    private LabelService service(LabelRepository repo, TodoRepository todoRepo, RoutineRepository routineRepo) {
        return new LabelService(repo, todoRepo, routineRepo);
    }

    @Test
    void 목록조회는_본인_라벨만_반환한다() {
        LabelRepository repo = mock(LabelRepository.class);
        LabelEntity a = LabelEntity.create(MEMBER_ID, "업무", "#FF0000");
        LabelEntity b = LabelEntity.create(MEMBER_ID, "운동", null);
        when(repo.findAllByMemberId(MEMBER_ID)).thenReturn(List.of(a, b));
        LabelService service = service(repo);

        List<Label> labels = service.getLabels(MEMBER_ID);

        assertThat(labels).hasSize(2);
        assertThat(labels).extracting(Label::name).containsExactlyInAnyOrder("업무", "운동");
    }

    @Test
    void 단건조회는_존재하지_않으면_예외() {
        LabelRepository repo = mock(LabelRepository.class);
        UUID id = UUID.randomUUID();
        when(repo.findByIdAndMemberId(id, MEMBER_ID)).thenReturn(Optional.empty());
        LabelService service = service(repo);

        assertThatThrownBy(() -> service.getLabel(MEMBER_ID, id))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void 다른_회원의_라벨을_조회하려하면_예외() {
        LabelRepository repo = mock(LabelRepository.class);
        UUID id = UUID.randomUUID();
        UUID otherMemberId = UUID.randomUUID();
        when(repo.findByIdAndMemberId(id, otherMemberId)).thenReturn(Optional.empty());
        LabelService service = service(repo);

        assertThatThrownBy(() -> service.getLabel(otherMemberId, id))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void 단건조회는_존재하면_반환한다() {
        LabelRepository repo = mock(LabelRepository.class);
        UUID id = UUID.randomUUID();
        LabelEntity existing = LabelEntity.create(MEMBER_ID, "업무", "#FF0000");
        when(repo.findByIdAndMemberId(id, MEMBER_ID)).thenReturn(Optional.of(existing));
        LabelService service = service(repo);

        Label label = service.getLabel(MEMBER_ID, id);

        assertThat(label.name()).isEqualTo("업무");
    }

    @Test
    void 등록하면_이름과_색상이_본인_소유로_반영된다() {
        LabelRepository repo = mock(LabelRepository.class);
        when(repo.save(any(LabelEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        LabelService service = service(repo);

        Label label = service.createLabel(MEMBER_ID, new LabelRequest("업무", "#FF0000"));

        assertThat(label.memberId()).isEqualTo(MEMBER_ID);
        assertThat(label.name()).isEqualTo("업무");
        assertThat(label.color()).isEqualTo("#FF0000");
    }

    @Test
    void 등록시_색상_미지정이면_null로_저장된다() {
        LabelRepository repo = mock(LabelRepository.class);
        when(repo.save(any(LabelEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        LabelService service = service(repo);

        Label label = service.createLabel(MEMBER_ID, new LabelRequest("운동", null));

        assertThat(label.color()).isNull();
    }

    @Test
    void 수정시_존재하지_않으면_예외() {
        LabelRepository repo = mock(LabelRepository.class);
        UUID id = UUID.randomUUID();
        when(repo.findByIdAndMemberId(id, MEMBER_ID)).thenReturn(Optional.empty());
        LabelService service = service(repo);

        assertThatThrownBy(() -> service.updateLabel(MEMBER_ID, id, new LabelUpdateRequest("새이름", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void 수정시_이름이_공백이면_예외() {
        LabelRepository repo = mock(LabelRepository.class);
        LabelService service = service(repo);

        assertThatThrownBy(() -> service.updateLabel(MEMBER_ID, UUID.randomUUID(), new LabelUpdateRequest("   ", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    @Test
    void 부분수정시_보내지_않은_필드는_유지된다() {
        LabelRepository repo = mock(LabelRepository.class);
        UUID id = UUID.randomUUID();
        LabelEntity existing = LabelEntity.create(MEMBER_ID, "기존이름", "#000000");
        when(repo.findByIdAndMemberId(id, MEMBER_ID)).thenReturn(Optional.of(existing));
        LabelService service = service(repo);

        Label label = service.updateLabel(MEMBER_ID, id, new LabelUpdateRequest(null, "#FFFFFF"));

        assertThat(label.name()).isEqualTo("기존이름");
        assertThat(label.color()).isEqualTo("#FFFFFF");
    }

    @Test
    void 삭제시_존재하지_않으면_예외() {
        LabelRepository repo = mock(LabelRepository.class);
        UUID id = UUID.randomUUID();
        when(repo.findByIdAndMemberId(id, MEMBER_ID)).thenReturn(Optional.empty());
        LabelService service = service(repo);

        assertThatThrownBy(() -> service.deleteLabel(MEMBER_ID, id))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void 삭제시_존재하면_리포지토리에서_삭제된다() {
        LabelRepository repo = mock(LabelRepository.class);
        TodoRepository todoRepo = mock(TodoRepository.class);
        RoutineRepository routineRepo = mock(RoutineRepository.class);
        UUID id = UUID.randomUUID();
        LabelEntity existing = LabelEntity.create(MEMBER_ID, "업무", null);
        when(repo.findByIdAndMemberId(id, MEMBER_ID)).thenReturn(Optional.of(existing));
        when(todoRepo.existsByLabelId(id)).thenReturn(false);
        when(routineRepo.existsByLabelId(id)).thenReturn(false);
        LabelService service = service(repo, todoRepo, routineRepo);

        service.deleteLabel(MEMBER_ID, id);

        verify(repo).delete(existing);
    }

    @Test
    void 삭제시_todo에서_사용중이면_예외() {
        LabelRepository repo = mock(LabelRepository.class);
        TodoRepository todoRepo = mock(TodoRepository.class);
        RoutineRepository routineRepo = mock(RoutineRepository.class);
        UUID id = UUID.randomUUID();
        LabelEntity existing = LabelEntity.create(MEMBER_ID, "업무", null);
        when(repo.findByIdAndMemberId(id, MEMBER_ID)).thenReturn(Optional.of(existing));
        when(todoRepo.existsByLabelId(id)).thenReturn(true);
        LabelService service = service(repo, todoRepo, routineRepo);

        assertThatThrownBy(() -> service.deleteLabel(MEMBER_ID, id))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
        verify(repo, never()).delete(any(LabelEntity.class));
    }

    @Test
    void 삭제시_routine에서_사용중이면_예외() {
        LabelRepository repo = mock(LabelRepository.class);
        TodoRepository todoRepo = mock(TodoRepository.class);
        RoutineRepository routineRepo = mock(RoutineRepository.class);
        UUID id = UUID.randomUUID();
        LabelEntity existing = LabelEntity.create(MEMBER_ID, "업무", null);
        when(repo.findByIdAndMemberId(id, MEMBER_ID)).thenReturn(Optional.of(existing));
        when(todoRepo.existsByLabelId(id)).thenReturn(false);
        when(routineRepo.existsByLabelId(id)).thenReturn(true);
        LabelService service = service(repo, todoRepo, routineRepo);

        assertThatThrownBy(() -> service.deleteLabel(MEMBER_ID, id))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
        verify(repo, never()).delete(any(LabelEntity.class));
    }
}
