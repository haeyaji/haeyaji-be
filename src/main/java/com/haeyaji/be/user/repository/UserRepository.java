package com.haeyaji.be.user.repository;

import com.haeyaji.be.user.domain.SocialType;
import com.haeyaji.be.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findBySocialTypeAndSocialTypeId(SocialType socialType, String socialTypeId);
}
