package com.dev.csas.course;

import com.dev.csas.audit.UserActionService;
import com.dev.csas.audit.UserActionType;
import com.dev.csas.api.CourseHeaderResponse;
import com.dev.csas.api.course.CourseCreationRequest;
import com.dev.csas.attendance.AttendanceSheetJPAEntity;
import com.dev.csas.attendance.AttendanceSheetRepository;
import com.dev.csas.attendance.AttendanceSheetStatus;
import com.dev.csas.control.entity.ControlSheetRepository;
import com.dev.csas.coursework.CourseworkSheetStatus;
import com.dev.csas.coursework.CourseworkSheetJPAEntity;
import com.dev.csas.coursework.CourseworkSheetRepository;
import com.dev.csas.discipline.DisciplineRepository;
import com.dev.csas.error.CatalogBadRequestException;
import com.dev.csas.error.CatalogConflictException;
import com.dev.csas.error.CatalogNotFoundException;
import com.dev.csas.group.GroupRepository;
import com.dev.csas.semester.SemesterRepository;
import com.dev.csas.student.StudentRepository;
import com.dev.csas.teacher.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

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
    private final ControlSheetRepository controlSheets;
    private final UserActionService userActions;

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
        userActions.record(UserActionType.COURSE_ASSIGNED, "COURSE", saved.getId(), null);

        return new CourseHeaderResponse(
                saved.getId(), discipline.getName(), group.getName(), startSemester.getName(),
                endSemester.getName(), saved.getCourseType(), saved.getStatus()
        );
    }

    @Transactional
    public CourseHeaderResponse update(long courseId, CourseCreationRequest request) {
        CourseJPAEntity entity = courses.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("Курс не найден: " + courseId));
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

        validatePeriod(startSemester, endSemester);
        if (entity.getCourseType() != request.courseType()) {
            throw new CatalogBadRequestException("Тип курса нельзя изменить после создания");
        }
        if (courses.existsByDiscipline_IdAndGroup_IdAndStartSemester_IdAndEndSemester_IdAndCourseTypeAndIdNot(
                discipline.getId(), group.getId(), startSemester.getId(), endSemester.getId(),
                request.courseType(), courseId
        )) {
            throw new CatalogConflictException("Такой курс для группы и периода уже существует");
        }

        boolean groupChanged = !entity.getGroup().getId().equals(group.getId());
        var changedFields = new ArrayList<String>();
        if (!entity.getDiscipline().getId().equals(discipline.getId())) {
            changedFields.add("дисциплина");
        }
        if (groupChanged) {
            changedFields.add("группа");
        }
        if (!entity.getStartSemester().getId().equals(startSemester.getId())
                || !entity.getEndSemester().getId().equals(endSemester.getId())) {
            changedFields.add("период");
        }
        if (!entity.getTeacher().getId().equals(teacher.getId())) {
            changedFields.add("преподаватель");
        }
        boolean assignmentIdentityChanged = groupChanged
                || !entity.getDiscipline().getId().equals(discipline.getId())
                || !entity.getStartSemester().getId().equals(startSemester.getId())
                || !entity.getEndSemester().getId().equals(endSemester.getId());
        if (assignmentIdentityChanged && entity.getStatus() == CourseStatus.COMPLETED) {
            throw new CatalogConflictException("Нельзя изменить завершённый курс");
        }
        if (assignmentIdentityChanged && hasRecordedData(entity)) {
            throw new CatalogConflictException(
                    "Нельзя изменить дисциплину, группу или период курса после появления заданий или ведомостей"
            );
        }

        entity.setDiscipline(discipline);
        entity.setGroup(group);
        entity.setStartSemester(startSemester);
        entity.setEndSemester(endSemester);
        entity.setTeacher(teacher);
        if (groupChanged) {
            entity.setStudents(students.findAllByGroup_IdOrderByIdAsc(group.getId()));
        }

        CourseJPAEntity saved = courses.saveAndFlush(entity);
        if (!changedFields.isEmpty()) {
            userActions.record(UserActionType.COURSE_UPDATED, "COURSE", saved.getId(),
                    "Изменены поля: " + String.join(", ", changedFields));
        }
        return toHeader(saved);
    }

    @Transactional
    public void delete(long courseId) {
        CourseJPAEntity entity = courses.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("Курс не найден: " + courseId));
        if (entity.getStatus() == CourseStatus.COMPLETED) {
            throw new CatalogConflictException("Нельзя удалить завершённый курс");
        }
        if (hasRecordedData(entity)) {
            throw new CatalogConflictException(
                    "Нельзя удалить курс, для которого уже созданы задания или ведомости"
            );
        }
        attendanceSheets.findByCourse_Id(courseId).ifPresent(attendanceSheets::delete);
        courseworkSheets.findByCourse_Id(courseId).ifPresent(courseworkSheets::delete);
        courses.delete(entity);
        userActions.record(UserActionType.COURSE_DELETED, "COURSE", courseId, null);
    }

    private boolean hasRecordedData(CourseJPAEntity course) {
        if (!course.getWorks().isEmpty() || controlSheets.existsByCourse_Id(course.getId())) {
            return true;
        }
        boolean hasAttendance = attendanceSheets.findByCourse_Id(course.getId())
                .map(sheet -> sheet.getStatus() != AttendanceSheetStatus.OPEN || !sheet.getLectures().isEmpty())
                .orElse(false);
        boolean hasCoursework = courseworkSheets.findByCourse_Id(course.getId())
                .map(sheet -> sheet.getStatus() != CourseworkSheetStatus.OPEN || !sheet.getResults().isEmpty())
                .orElse(false);
        return hasAttendance || hasCoursework;
    }

    private void validatePeriod(com.dev.csas.semester.SemesterJPAEntity startSemester,
                                com.dev.csas.semester.SemesterJPAEntity endSemester) {
        if (startSemester.getStartDate().isAfter(endSemester.getStartDate())) {
            throw new CatalogBadRequestException("Семестр начала курса не может быть позже семестра окончания");
        }
    }

    private CourseHeaderResponse toHeader(CourseJPAEntity course) {
        return new CourseHeaderResponse(
                course.getId(), course.getDiscipline().getName(), course.getGroup().getName(),
                course.getStartSemester().getName(), course.getEndSemester().getName(),
                course.getCourseType(), course.getStatus()
        );
    }
}
