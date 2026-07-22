package com.haeyaji.be.label.service;

import com.haeyaji.be.common.exception.BusinessException;
import com.haeyaji.be.common.exception.ErrorCode;
import com.haeyaji.be.label.domain.Label;
import com.haeyaji.be.label.dto.LabelRequest;
import com.haeyaji.be.label.dto.LabelUpdateRequest;
import com.haeyaji.be.label.repository.LabelEntity;
import com.haeyaji.be.label.repository.LabelRepository;
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

    public List<Label> getLabels() {
        return labelRepository.findAll().stream()
                .map(LabelEntity::toDomain)
                .toList();
    }

    public Label getLabel(UUID id) {
        return labelRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND))
                .toDomain();
    }

    @Transactional
    public Label createLabel(LabelRequest request) {
        LabelEntity entity = LabelEntity.create(null, request.name(), request.color());
        return labelRepository.save(entity).toDomain();
    }

    @Transactional
    public Label updateLabel(UUID id, LabelUpdateRequest request) {
        if (request.name() != null && request.name().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
        LabelEntity entity = labelRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        entity.update(request.name(), request.color());
        return entity.toDomain();
    }

    @Transactional
    public void deleteLabel(UUID id) {
        LabelEntity entity = labelRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        labelRepository.delete(entity);
    }
}
