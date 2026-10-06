package com.dev.csas.teacher;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface TeacherRepository extends JpaRepository<TeacherJPAEntity, Long> {

    Optional<TeacherJPAEntity> findByUser_Id(Long userId);

    List<TeacherJPAEntity> findAllByUserIsNullOrderByIdAsc();
}
