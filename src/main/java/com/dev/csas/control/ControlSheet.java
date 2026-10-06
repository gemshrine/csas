package com.dev.csas.control;

import com.dev.csas.control.audit.ControlActorType;
import com.dev.csas.control.audit.ControlEventType;
import com.dev.csas.control.audit.ControlSheetEvent;
import com.dev.csas.course.Course;
import com.dev.csas.course.CourseNotCompletedException;
import com.dev.csas.course.CourseType;
import com.dev.csas.course.InvalidCourseTypeException;
import com.dev.csas.course.NonExistentStudentException;
import com.dev.csas.discipline.ControlFormEnum;
import com.dev.csas.shared.DeanID;
import com.dev.csas.shared.SheetStatusException;
import com.dev.csas.shared.StudentID;
import com.dev.csas.shared.TeacherID;

import java.time.Instant;
import java.util.*;

public class ControlSheet {

    private final Course course;
    private final ControlFormEnum controlForm;
    private final GradingScale gradingScale;
    private final Map<StudentID, ControlResult> results = new HashMap<>();
    private ControlSheetStatus status = ControlSheetStatus.OPEN;

    private final List<ControlSheetEvent> events = new ArrayList<>();

    public ControlSheet(Course course, ControlFormEnum controlForm, GradingScale gradingScale) {
        this.course = Objects.requireNonNull(course);
        validateCourseType(course);
        if (!course.isCompleted()) {
            throw new CourseNotCompletedException("Нельзя открыть ведомость для незавершённого курса");
        }
        this.controlForm = Objects.requireNonNull(controlForm);
        this.gradingScale = Objects.requireNonNull(gradingScale);
    }

    private ControlSheet(
            Course course,
            ControlFormEnum controlForm,
            GradingScale gradingScale,
            ControlSheetStatus status,
            Map<StudentID, ControlResult> results
    ) {
        this.course = Objects.requireNonNull(course);
        validateCourseType(course);
        this.controlForm = Objects.requireNonNull(controlForm);
        this.gradingScale = Objects.requireNonNull(gradingScale);
        this.status = Objects.requireNonNull(status);
        this.results.putAll(results);
    }

    static ControlSheet reconstitute(
            Course course,
            ControlFormEnum controlForm,
            GradingScale gradingScale,
            ControlSheetStatus status,
            Map<StudentID, ControlResult> results
    ) {
        return new ControlSheet(course, controlForm, gradingScale, status, results);
    }

    public void enterNumericResult(TeacherID teacher, StudentID student, int score) {
        enterNumericResult(teacher, student, score, null);
    }

    public void enterNumericResult(TeacherID teacher, StudentID student, int score, String reason) {
        validateEntry(teacher);
        validateStudent(student);

        if (controlForm == ControlFormEnum.CREDIT) {
            throw new InvalidControlResultException("Для обычного зачёта нужен результат зачёт/незачёт");
        }

        if (score < 0 || score > 100) {
            throw new InvalidControlResultException("Результат контроля должен быть от 0 до 100");
        }

        saveResult(teacher, student, ControlResult.numeric(score), reason);
    }

    public void enterPassFailResult(TeacherID teacher, StudentID student, PassFail result) {
        enterPassFailResult(teacher, student, result, null);
    }

    public void enterPassFailResult(TeacherID teacher, StudentID student, PassFail result, String reason) {
        validateEntry(teacher);
        validateStudent(student);

        if (controlForm != ControlFormEnum.CREDIT) {
            throw new InvalidControlResultException("Для экзамена и диф. зачёта нужен числовой результат");
        }

        saveResult(
                teacher,
                student,
                ControlResult.passFail(Objects.requireNonNull(result)),
                reason
        );
    }

    public ControlResult resultOf(StudentID student) {
        validateStudent(student);

        ControlResult result = results.get(student);

        if (result == null) {
            throw new ControlSheetException("Результат контроля не внесён");
        }

        return result;
    }

    public double finalScore(StudentID student) {
        if (controlForm == ControlFormEnum.CREDIT) {
            throw new InvalidControlResultException("Для обычного зачёта числовой итог не рассчитывается");
        }

        return (course.rating(student) + resultOf(student).numericScore()) / 2.0;
    }

    public int finalGrade(StudentID student) {
        if (status == ControlSheetStatus.OPEN) {
            throw new ControlSheetException("Итоговую оценку можно получить только из закрытой ведомости");
        }

        return gradingScale.grade(finalScore(student));
    }

    public void close(TeacherID teacher) {
        validateEntry(teacher);

        List<StudentID> students = course.students();
        if (!results.keySet().containsAll(students)) {
            throw new IncompleteControlSheetException("Нельзя закрыть ведомость без результатов всех студентов");
        }

        status = ControlSheetStatus.CLOSED;
        recordEvent(
                ControlActorType.TEACHER,
                ControlEventType.CLOSED,
                teacher.id(),
                null,
                null,
                null,
                null
        );
    }

    public void reopen(DeanID dean) {
        reopen(dean, null);
    }

    public void reopen(DeanID dean, String reason) {
        Objects.requireNonNull(dean, "Деканат обязателен");
        if (status != ControlSheetStatus.CLOSED) {
            throw new SheetStatusException("Переоткрыть можно только закрытую ведомость");
        }

        status = ControlSheetStatus.OPEN;
        recordEvent(
                ControlActorType.DEAN,
                ControlEventType.REOPENED,
                dean.id(),
                null,
                null,
                null,
                reason
        );
    }

    public void archive(DeanID dean) {
        archive(dean, null);
    }

    public void archive(DeanID dean, String reason) {
        Objects.requireNonNull(dean, "Деканат обязателен");
        if (status != ControlSheetStatus.CLOSED) {
            throw new SheetStatusException("Архивировать можно только закрытую ведомость");
        }

        status = ControlSheetStatus.ARCHIVED;
        recordEvent(
                ControlActorType.DEAN,
                ControlEventType.ARCHIVED,
                dean.id(),
                null,
                null,
                null,
                reason
        );
    }

    public ControlSheetStatus status() {
        return status;
    }

    public ControlFormEnum controlForm() {
        return controlForm;
    }

    public GradingScale gradingScale() {
        return gradingScale;
    }

    public Map<StudentID, ControlResult> results() {
        return Map.copyOf(results);
    }

    public List<ControlSheetEvent> events() {
        return List.copyOf(events);
    }

    private void validateEntry(TeacherID teacher) {
        if (status != ControlSheetStatus.OPEN) {
            throw new ControlSheetException("Ведомость закрыта или архивирована");
        }

        if (!teacher.equals(course.teacher())) {
            throw new UnauthorizedTeacherException("Результат может внести только преподаватель курса");
        }
    }

    private static void validateCourseType(Course course) {
        if (course.type() != CourseType.SEMINAR) {
            throw new InvalidCourseTypeException("Контрольная ведомость создаётся только для семинарного курса");
        }
    }

    private void validateStudent(StudentID student) {
        if (!course.hasStudent(student)) {
            throw new NonExistentStudentException("Студент не относится к курсу: " + student.id());
        }
    }

    private void saveResult(
            TeacherID teacher,
            StudentID student,
            ControlResult newResult,
            String reason
    ) {
        ControlResult oldResult = results.put(student, newResult);

        if (Objects.equals(oldResult, newResult)) {
            return;
        }

        recordEvent(
                ControlActorType.TEACHER,
                oldResult == null ? ControlEventType.RESULT_ENTERED : ControlEventType.RESULT_CHANGED,
                teacher.id(),
                student,
                oldResult,
                newResult,
                reason
        );
    }

    private void recordEvent(
            ControlActorType actorType,
            ControlEventType eventType,
            long actorId,
            StudentID studentID,
            ControlResult oldResult,
            ControlResult newResult,
            String reason
    ) {
        events.add(new ControlSheetEvent(
                actorType,
                eventType,
                actorId,
                studentID,
                oldResult,
                newResult,
                reason,
                Instant.now()
        ));
    }
}
