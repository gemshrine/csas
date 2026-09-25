package com.dev.csas.student;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<StudentJPAEntity, Long> {

    boolean existsByGroup_Id(Long groupId);

    boolean existsByRecordBookNumber(String recordBookNumber);

    boolean existsByRecordBookNumberAndIdNot(String recordBookNumber, Long id);
}
