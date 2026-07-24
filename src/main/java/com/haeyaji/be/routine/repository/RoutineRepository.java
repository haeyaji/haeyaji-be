package com.haeyaji.be.routine.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoutineRepository extends JpaRepository<RoutineEntity, UUID> {

    /** routineDays가 LAZY라 조회 때마다 요일을 접근하면 N+1이 나서, 전 회원 스케줄러/일괄조회 경로엔 fetch join을 강제한다(M3). */
    @EntityGraph(attributePaths = "routineDays")
    List<RoutineEntity> findByActiveTrue();

    @EntityGraph(attributePaths = "routineDays")
    List<RoutineEntity> findByActiveTrueAndMemberId(UUID memberId);

    @EntityGraph(attributePaths = "routineDays")
    List<RoutineEntity> findAllByMemberId(UUID memberId);

    @EntityGraph(attributePaths = "routineDays")
    Optional<RoutineEntity> findByIdAndMemberId(UUID id, UUID memberId);

    boolean existsByLabelId(UUID labelId);
}