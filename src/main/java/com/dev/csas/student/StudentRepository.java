package com.dev.csas.student;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface StudentRepository extends JpaRepository<StudentJPAEntity, Long> {

    Optional<StudentJPAEntity> findByUser_Id(Long userId);

    boolean existsByGroup_Id(Long groupId);

    List<StudentJPAEntity> findAllByGroup_IdOrderByIdAsc(Long groupId);

    boolean existsByRecordBookNumber(String recordBookNumber);

    boolean existsByRecordBookNumberAndIdNot(String recordBookNumber, Long id);
}
