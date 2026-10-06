package com.dev.csas.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserActionRepository extends JpaRepository<UserActionJPAEntity, Long> {
    List<UserActionJPAEntity> findTop200ByOrderByCreatedAtDescIdDesc();
}
