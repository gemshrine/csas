package com.dev.csas.course;

import com.dev.csas.api.course.CourseDetailsResponse;
import com.dev.csas.api.course.CourseWorkRequest;
import com.dev.csas.error.CatalogBadRequestException;
import com.dev.csas.error.CatalogNotFoundException;
import com.dev.csas.security.ActorAccessDeniedException;
import com.dev.csas.shared.DeanID;
import com.dev.csas.shared.StudentID;
import com.dev.csas.shared.TeacherID;
import com.dev.csas.semester.SemesterJPAEntity;
import com.dev.csas.semester.SemesterRepository;
import com.dev.csas.student.StudentJPAEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CourseApplicationService {

    private final CourseRepository courses;
    private final CourseMapper mapper;
    private final SemesterRepository semesters;

    public CourseApplicationService(
            CourseRepository courses,
            CourseMapper mapper,
            SemesterRepository semesters
    ) {
        this.courses = courses;
        this.mapper = mapper;
        this.semesters = semesters;
    }

    @Transactional(readOnly = true)
    public CourseDetailsResponse getForTeacher(long courseId, TeacherID teacher) {
        CourseJPAEntity entity = findCourse(courseId);
        validateCourseTeacher(entity, teacher);
        return toResponse(entity, mapper.toDomain(entity));
    }

    @Transactional(readOnly = true)
    public CourseDetailsResponse getForDean(long courseId) {
        CourseJPAEntity entity = findCourse(courseId);
        return toResponse(entity, mapper.toDomain(entity));
    }

    @Transactional
    public long addWork(long courseId, TeacherID teacher, CourseWorkRequest request) {
        CourseJPAEntity entity = findCourse(courseId);
        validateSeminarTeacher(entity, teacher);
        Course course = mapper.toDomain(entity);
        long number = course.addWork(request.title(), request.maxScore(), request.date());
        persist(course, entity);
        return number;
    }

    @Transactional
    public void updateWork(long courseId, long workNumber, TeacherID teacher, CourseWorkRequest request) {
        CourseJPAEntity entity = findCourse(courseId);
        validateSeminarTeacher(entity, teacher);
        Course course = mapper.toDomain(entity);
        course.updateWorkTitle(workNumber, request.title());
        course.updateWorkMaxScore(workNumber, request.maxScore());
        course.updateWorkDate(workNumber, request.date());
        persist(course, entity);
    }

    @Transactional
    public void enterScore(long courseId, long workNumber, long studentId, TeacherID teacher, int score) {
        CourseJPAEntity entity = findCourse(courseId);
        validateSeminarTeacher(entity, teacher);
        Course course = mapper.toDomain(entity);
        course.score(workNumber, new StudentID(studentId), score);
        persist(course, entity);
    }

    @Transactional
    public void complete(long courseId, TeacherID teacher) {
        CourseJPAEntity entity = findCourse(courseId);
        validateCourseTeacher(entity, teacher);
        Course course = mapper.toDomain(entity);
        course.complete();
        persist(course, entity);
    }

    @Transactional
    public void reopen(long courseId, DeanID dean, long semesterId) {
        CourseJPAEntity entity = findCourse(courseId);
        Course course = mapper.toDomain(entity);
        course.reopen(dean);

        var semester = semesters.findById(semesterId)
                .orElseThrow(() -> CatalogNotFoundException.semester(semesterId));
        var nextSemester = nextSemester(entity)
                .orElseThrow(() -> new CatalogBadRequestException(
                        "Для курса не настроен следующий семестр пересдачи"
                ));
        if (!nextSemester.getId().equals(semester.getId())) {
            throw new CatalogBadRequestException("Курс можно переоткрыть только в следующем семестре");
        }

        entity.setEndSemester(semester);
        persist(course, entity);
    }

    @Transactional
    public void reopenInNextSemester(long courseId, DeanID dean) {
        CourseJPAEntity entity = findCourse(courseId);
        Course course = mapper.toDomain(entity);
        course.reopen(dean);

        SemesterJPAEntity semester = nextSemester(entity)
                .orElseThrow(() -> new CatalogBadRequestException(
                        "Для курса не настроен следующий семестр пересдачи"
                ));
        entity.setEndSemester(semester);
        persist(course, entity);
    }

    @Transactional(readOnly = true)
    public boolean canReopenInNextSemester(long courseId) {
        CourseJPAEntity entity = findCourse(courseId);
        return entity.getStatus() == CourseStatus.COMPLETED && nextSemester(entity).isPresent();
    }

    private CourseJPAEntity findCourse(long courseId) {
        return courses.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("Курс не найден: " + courseId));
    }

    private Optional<SemesterJPAEntity> nextSemester(CourseJPAEntity course) {
        return semesters.findFirstByStartDateAfterOrderByStartDateAsc(
                course.getEndSemester().getEndDate()
        );
    }

    private void validateSeminarTeacher(CourseJPAEntity course, TeacherID teacher) {
        if (course.getCourseType() != CourseType.SEMINAR || !course.getTeacher().getId().equals(teacher.id())) {
            throw new ActorAccessDeniedException("Операция доступна только преподавателю семинарской части курса");
        }
    }

    private void validateCourseTeacher(CourseJPAEntity course, TeacherID teacher) {
        if (!course.getTeacher().getId().equals(teacher.id())) {
            throw new ActorAccessDeniedException("Нет доступа к этому курсу");
        }
    }

    private void persist(Course course, CourseJPAEntity entity) {
        Map<Long, StudentJPAEntity> students = entity.getStudents().stream()
                .collect(Collectors.toMap(StudentJPAEntity::getId, Function.identity()));
        mapper.updateOwnedData(course, entity, studentID -> students.get(studentID.id()));
    }

    private CourseDetailsResponse toResponse(CourseJPAEntity entity, Course course) {
        List<CourseDetailsResponse.CourseWorkResponse> works = course.works().stream()
                .sorted(Comparator.comparingLong(Work::getNumber))
                .map(work -> new CourseDetailsResponse.CourseWorkResponse(
                        work.getNumber(),
                        work.getTitle(),
                        work.getMaxScore(),
                        work.getDate(),
                        course.scores().stream()
                                .filter(score -> score.getWorkNumber() == work.getNumber())
                                .map(score -> new CourseDetailsResponse.CourseWorkScoreResponse(
                                        score.getStudentId().id(), score.getScore()
                                ))
                                .toList()
                ))
                .toList();
        List<CourseDetailsResponse.CourseStudentResponse> students = entity.getStudents().stream()
                .sorted(Comparator.comparing(StudentJPAEntity::getId))
                .map(student -> new CourseDetailsResponse.CourseStudentResponse(
                        student.getId(),
                        student.getLastName() + " " + student.getFirstName()
                                + (student.getPatronymic() == null ? "" : " " + student.getPatronymic()),
                        course.type() == CourseType.SEMINAR
                                ? course.rating(new StudentID(student.getId()))
                                : null
                ))
                .toList();
        return new CourseDetailsResponse(
                entity.getId(), entity.getDiscipline().getName(), entity.getGroup().getName(),
                entity.getStartSemester().getName(), entity.getEndSemester().getName(),
                course.type(), course.status(), works, students
        );
    }
}
