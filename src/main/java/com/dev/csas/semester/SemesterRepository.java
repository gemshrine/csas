package com.dev.csas.semester;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface SemesterRepository extends JpaRepository<SemesterJPAEntity, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    Optional<SemesterJPAEntity> findFirstByStartDateAfterOrderByStartDateAsc(LocalDate date);
}
