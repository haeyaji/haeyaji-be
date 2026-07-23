package com.haeyaji.be.profile.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MemberPreferenceRepository extends JpaRepository<MemberPreferenceEntity, UUID> {
}
