package com.dev.csas.semester;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SemesterRepository extends JpaRepository<SemesterJPAEntity, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
