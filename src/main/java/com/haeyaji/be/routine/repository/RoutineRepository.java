package com.haeyaji.be.routine.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoutineRepository extends JpaRepository<RoutineEntity, UUID> {

    List<RoutineEntity> findByActiveTrue();

    List<RoutineEntity> findByActiveTrueAndMemberId(UUID memberId);

    List<RoutineEntity> findAllByMemberId(UUID memberId);

    Optional<RoutineEntity> findByIdAndMemberId(UUID id, UUID memberId);

    boolean existsByLabelId(UUID labelId);
}