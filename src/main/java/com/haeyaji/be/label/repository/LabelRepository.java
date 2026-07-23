package com.haeyaji.be.label.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LabelRepository extends JpaRepository<LabelEntity, UUID> {

    List<LabelEntity> findAllByMemberId(UUID memberId);

    Optional<LabelEntity> findByIdAndMemberId(UUID id, UUID memberId);
}
