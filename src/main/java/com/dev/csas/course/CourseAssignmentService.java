package com.dev.csas.course;

import com.dev.csas.api.CourseHeaderResponse;
import com.dev.csas.api.course.CourseCreationRequest;
import com.dev.csas.attendance.AttendanceSheetJPAEntity;
import com.dev.csas.attendance.AttendanceSheetRepository;
import com.dev.csas.coursework.CourseworkSheetJPAEntity;
import com.dev.csas.coursework.CourseworkSheetRepository;
import com.dev.csas.discipline.DisciplineRepository;
import com.dev.csas.error.CatalogBadRequestException;
import com.dev.csas.error.CatalogNotFoundException;
import com.dev.csas.group.GroupRepository;
import com.dev.csas.semester.SemesterRepository;
import com.dev.csas.student.StudentRepository;
import com.dev.csas.teacher.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseAssignmentService {

    private final CourseRepository courses;
    private final DisciplineRepository disciplines;
    private final GroupRepository groups;
    private final SemesterRepository semesters;
    private final TeacherRepository teachers;
    private final StudentRepository students;
    private final CourseworkSheetRepository courseworkSheets;
    private final AttendanceSheetRepository attendanceSheets;

    @Transactional
    public CourseHeaderResponse assign(CourseCreationRequest request) {
        var discipline = disciplines.findById(request.disciplineId())
                .orElseThrow(() -> CatalogNotFoundException.discipline(request.disciplineId()));
        var group = groups.findById(request.groupId())
                .orElseThrow(() -> CatalogNotFoundException.group(request.groupId()));
        var startSemester = semesters.findById(request.startSemesterId())
                .orElseThrow(() -> CatalogNotFoundException.semester(request.startSemesterId()));
        var endSemester = semesters.findById(request.endSemesterId())
                .orElseThrow(() -> CatalogNotFoundException.semester(request.endSemesterId()));
        var teacher = teachers.findById(request.teacherId())
                .orElseThrow(() -> CatalogNotFoundException.teacher(request.teacherId()));

        if (startSemester.getStartDate().isAfter(endSemester.getStartDate())) {
            throw new CatalogBadRequestException("Семестр начала курса не может быть позже семестра окончания");
        }
        var groupStudents = students.findAllByGroup_IdOrderByIdAsc(group.getId());

        CourseJPAEntity entity = CourseJPAEntity.builder()
                .discipline(discipline)
                .group(group)
                .startSemester(startSemester)
                .endSemester(endSemester)
                .courseType(request.courseType())
                .teacher(teacher)
                .students(groupStudents)
                .build();
        CourseJPAEntity saved = courses.saveAndFlush(entity);

        if (request.courseType() == CourseType.COURSEWORK) {
            courseworkSheets.save(CourseworkSheetJPAEntity.builder().course(saved).build());
        }
        if (request.courseType() == CourseType.LECTURE) {
            attendanceSheets.save(AttendanceSheetJPAEntity.builder().course(saved).build());
        }

        return new CourseHeaderResponse(
                saved.getId(), discipline.getName(), group.getName(), startSemester.getName(),
                endSemester.getName(), saved.getCourseType(), saved.getStatus()
        );
    }
}
