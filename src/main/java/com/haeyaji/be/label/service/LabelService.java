package com.haeyaji.be.label.service;

import com.haeyaji.be.label.domain.Label;
import com.haeyaji.be.label.dto.LabelRequest;
import com.haeyaji.be.label.repository.LabelEntity;
import com.haeyaji.be.label.repository.LabelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LabelService {

    private final LabelRepository labelRepository;

    @Transactional
    public Label createLabel(LabelRequest request) {
        LabelEntity entity = LabelEntity.create(null, request.name(), request.color());
        return labelRepository.save(entity).toDomain();
    }
}
