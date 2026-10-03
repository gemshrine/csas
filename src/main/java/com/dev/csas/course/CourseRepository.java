package com.dev.csas.course;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<CourseJPAEntity, Long> {

    @EntityGraph(attributePaths = {"discipline", "group", "startSemester", "endSemester"})
    List<CourseJPAEntity> findAllByTeacher_IdOrderByIdAsc(Long teacherId);

    @EntityGraph(attributePaths = {"discipline", "group", "startSemester", "endSemester"})
    List<CourseJPAEntity> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = {"discipline", "group", "startSemester", "endSemester"})
    List<CourseJPAEntity> findAllByStudents_IdOrderByIdAsc(Long studentId);

    boolean existsByStartSemester_Id(Long semesterId);

    boolean existsByEndSemester_Id(Long semesterId);


    boolean existsByGroup_Id(Long groupId);

    boolean existsByDiscipline_Id(Long disciplineId);

    boolean existsByTeacher_Id(Long teacherId);

    boolean existsByStudents_Id(Long studentId);
}
