package com.dev.csas.control;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ControlResultRepository extends JpaRepository<ControlResultJPAEntity, Long> {

    boolean existsByStudent_Id(Long studentId);
}
