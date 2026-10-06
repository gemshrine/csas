package com.dev.csas.group;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<GroupJPAEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "curator")
    List<GroupJPAEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "curator")
    Optional<GroupJPAEntity> findById(Long id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByCurator_Id(Long userId);
}
