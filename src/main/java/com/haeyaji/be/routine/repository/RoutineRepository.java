package com.haeyaji.be.routine.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoutineRepository extends JpaRepository<RoutineEntity, UUID> {
}
