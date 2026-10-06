package com.dev.csas.coursework;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CourseworkSheetRepository extends JpaRepository<CourseworkSheetJPAEntity, Long> {

    @EntityGraph(attributePaths = {
            "course.discipline", "course.group", "course.startSemester", "course.endSemester",
            "course.teacher", "results", "results.student"
    })
    Optional<CourseworkSheetJPAEntity> findDetailedById(Long id);

    Optional<CourseworkSheetJPAEntity> findByCourse_Id(Long courseId);

    boolean existsByCourse_Id(Long courseId);

    @Query("select new com.dev.csas.coursework.CourseworkSheetReadModel(s.course.id, s.id, s.status) " +
            "from CourseworkSheetJPAEntity s where s.course.id in :courseIds")
    List<CourseworkSheetReadModel> findCourseSheetReadModels(@Param("courseIds") Collection<Long> courseIds);

    @Query("select new com.dev.csas.coursework.CourseworkStudentResultReadModel(" +
            "s.course.id, r.student.id, r.score) from CourseworkSheetJPAEntity s join s.results r " +
            "where s.course.id in :courseIds and r.student.id = :studentId")
    List<CourseworkStudentResultReadModel> findStudentResults(
            @Param("courseIds") Collection<Long> courseIds,
            @Param("studentId") long studentId
    );

    @Query("select r from CourseworkSheetJPAEntity s join s.results r " +
            "where s.course.id = :courseId and r.student.id = :studentId")
    Optional<CourseworkResultJPAEntity> findStudentResult(
            @Param("courseId") long courseId,
            @Param("studentId") long studentId
    );
}
