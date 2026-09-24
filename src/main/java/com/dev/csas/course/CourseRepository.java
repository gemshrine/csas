package com.dev.csas.course;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<CourseJPAEntity, Long> {

    @EntityGraph(attributePaths = {"discipline", "group", "semester"})
    List<CourseJPAEntity> findAllBySeminarTeacher_IdOrLectureTeacher_IdOrderByIdAsc(
            Long seminarTeacherId,
            Long lectureTeacherId
    );

    @EntityGraph(attributePaths = {"discipline", "group", "semester"})
    List<CourseJPAEntity> findAllByOrderByIdAsc();
}
