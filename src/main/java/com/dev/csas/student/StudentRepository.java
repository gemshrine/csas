package com.dev.csas.student;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<StudentJPAEntity, Long> {

    Optional<StudentJPAEntity> findByUser_Id(Long userId);

    boolean existsByGroup_Id(Long groupId);

    boolean existsByRecordBookNumber(String recordBookNumber);

    boolean existsByRecordBookNumberAndIdNot(String recordBookNumber, Long id);
}
