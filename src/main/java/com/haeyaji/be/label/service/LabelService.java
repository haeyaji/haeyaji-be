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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LabelService {

    private final LabelRepository labelRepository;
    private final TodoRepository todoRepository;
    private final RoutineRepository routineRepository;

    public List<Label> getLabels(UUID memberId) {
        return labelRepository.findAllByMemberId(memberId).stream()
                .map(LabelEntity::toDomain)
                .toList();
    }

    public Label getLabel(UUID memberId, UUID id) {
        return labelRepository.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND))
                .toDomain();
    }

    @Transactional
    public Label createLabel(UUID memberId, LabelRequest request) {
        LabelEntity entity = LabelEntity.create(memberId, request.name(), request.color());
        return labelRepository.save(entity).toDomain();
    }

    @Transactional
    public Label updateLabel(UUID memberId, UUID id, LabelUpdateRequest request) {
        if (request.name() != null && request.name().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
        LabelEntity entity = labelRepository.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        entity.update(request.name(), request.color());
        return entity.toDomain();
    }

    /**
     * label_id는 todo/routine에서 실제 FK 연관관계가 아니라 느슨한 UUID 컬럼이라(N4),
     * 라벨을 그냥 지우면 그 컬럼들이 존재하지 않는 라벨을 가리키는 고아 참조로 남는다.
     * DB가 대신 막아주지 않으므로, 사용 중인 라벨은 삭제 자체를 거부한다.
     */
    @Transactional
    public void deleteLabel(UUID memberId, UUID id) {
        LabelEntity entity = labelRepository.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (todoRepository.existsByLabelId(id) || routineRepository.existsByLabelId(id)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
        labelRepository.delete(entity);
    }
}
