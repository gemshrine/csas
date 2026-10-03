package com.dev.csas.control.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.dev.csas.control.ControlSheetReadModel;
import com.dev.csas.control.ControlResultReadModel;
import com.dev.csas.student.StudentReadModel;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface ControlSheetRepository extends JpaRepository<ControlSheetJPAEntity, Long> {

    boolean existsByCourse_Id(Long courseId);

    @EntityGraph(attributePaths = {"course.discipline", "course.group", "course.startSemester", "course.endSemester", "course.teacher"})
    Optional<ControlSheetJPAEntity> findDetailedById(Long id);

    Optional<ControlSheetJPAEntity> findByCourse_Id(Long courseId);

    @Query("select new com.dev.csas.control.ControlSheetReadModel(s.course.id, s.id, s.status) " +
            "from ControlSheetJPAEntity s where s.course.id in :courseIds")
    List<ControlSheetReadModel> findCourseSheetReadModels(@Param("courseIds") Collection<Long> courseIds);

    @Query("select new com.dev.csas.control.ControlResultReadModel(r.student.id, r.numericScore, r.passFail) " +
            "from ControlResultJPAEntity r where r.controlSheet.id = :sheetId")
    List<ControlResultReadModel> findResultReadModelsBySheetId(@Param("sheetId") long sheetId);

    @Query("select new com.dev.csas.student.StudentReadModel(s.id, s.firstName, s.lastName, s.patronymic) " +
            "from ControlSheetJPAEntity sh join sh.course.students s " +
            "where sh.id = :sheetId order by s.lastName, s.firstName, s.patronymic, s.id")
    List<StudentReadModel> findStudentReadModelsBySheetId(@Param("sheetId") long sheetId);
}
