package com.haeyaji.be.label.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LabelRepository extends JpaRepository<LabelEntity, UUID> {
}
