package com.dev.csas.course;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<CourseJPAEntity, Long> {

    boolean existsByGroup_Id(Long groupId);

    boolean existsByDiscipline_Id(Long disciplineId);

    boolean existsBySemester_Id(Long semesterId);

    boolean existsBySeminarTeacher_IdOrLectureTeacher_Id(Long seminarTeacherId, Long lectureTeacherId);
}
