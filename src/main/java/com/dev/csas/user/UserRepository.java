package com.dev.csas.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserJPAEntity, Long> {
    Optional<UserJPAEntity> findByUsername(String username);

    List<UserJPAEntity> findAllByOrderByIdAsc();

    List<UserJPAEntity> findAllByRoleAndEnabledTrueOrderByIdAsc(RoleEnum role);

    long countByRoleAndEnabledTrue(RoleEnum role);
}
