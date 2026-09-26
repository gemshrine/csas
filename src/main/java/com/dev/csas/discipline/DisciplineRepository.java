package com.dev.csas.discipline;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DisciplineRepository extends JpaRepository<DisciplineJPAEntity, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
