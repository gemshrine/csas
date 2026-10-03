package com.dev.csas.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.dev.csas.student.StudentReadModel;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface AttendanceSheetRepository extends JpaRepository<AttendanceSheetJPAEntity, Long> {

    @EntityGraph(attributePaths = {"course.discipline", "course.group", "course.startSemester", "course.endSemester", "course.teacher"})
    Optional<AttendanceSheetJPAEntity> findDetailedById(Long id);

    @Query("select new com.dev.csas.attendance.AttendanceSheetReadModel(s.course.id, s.id, s.status) " +
            "from AttendanceSheetJPAEntity s where s.course.id in :courseIds")
    List<AttendanceSheetReadModel> findCourseSheetReadModels(@Param("courseIds") Collection<Long> courseIds);

    @Query("select new com.dev.csas.student.StudentReadModel(s.id, s.firstName, s.lastName, s.patronymic) " +
            "from AttendanceSheetJPAEntity sh join sh.course.students s " +
            "where sh.id = :sheetId order by s.lastName, s.firstName, s.patronymic, s.id")
    List<StudentReadModel> findStudentReadModelsBySheetId(@Param("sheetId") long sheetId);

    @Query("select new com.dev.csas.attendance.LectureReadModel(l.id, l.lectureDate, l.title) " +
            "from LectureJPAEntity l where l.attendanceSheet.id = :sheetId order by l.lectureDate, l.id")
    List<LectureReadModel> findLectureReadModelsBySheetId(@Param("sheetId") long sheetId);

    @Query("select new com.dev.csas.attendance.AttendanceMarkReadModel(l.id, a.student.id, a.wasPresent) " +
            "from LectureAttendanceJPAEntity a join a.lecture l where l.attendanceSheet.id = :sheetId")
    List<AttendanceMarkReadModel> findAttendanceMarkReadModelsBySheetId(@Param("sheetId") long sheetId);
}
