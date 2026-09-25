package com.dev.csas.course;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<CourseJPAEntity, Long> {

    // === Методы из main (для выборки курсов с подгрузкой связей) ===

    @EntityGraph(attributePaths = {"discipline", "group", "semester"})
    List<CourseJPAEntity> findAllBySeminarTeacher_IdOrLectureTeacher_IdOrderByIdAsc(
            Long seminarTeacherId,
            Long lectureTeacherId
    );

    @EntityGraph(attributePaths = {"discipline", "group", "semester"})
    List<CourseJPAEntity> findAllByOrderByIdAsc();

    // === Методы из твоей ветки (для проверок перед удалением/созданием) ===

    boolean existsByGroup_Id(Long groupId);

    boolean existsByDiscipline_Id(Long disciplineId);

    boolean existsBySemester_Id(Long semesterId);

    boolean existsBySeminarTeacher_IdOrLectureTeacher_Id(Long seminarTeacherId, Long lectureTeacherId);
}