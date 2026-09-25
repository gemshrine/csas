package com.dev.csas.control;

import com.dev.csas.control.audit.ControlActorType;
import com.dev.csas.control.audit.ControlEventType;
import com.dev.csas.control.audit.ControlSheetEvent;
import com.dev.csas.course.Course;
import com.dev.csas.course.CourseNotCompletedException;
import com.dev.csas.discipline.ControlFormEnum;
import com.dev.csas.shared.DeanID;
import com.dev.csas.shared.SheetStatusException;
import com.dev.csas.shared.StudentID;
import com.dev.csas.shared.TeacherID;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ControlSheetTest {

    private static final StudentID student = new StudentID(1);
    private static final TeacherID seminarTeacher = new TeacherID(10);
    private static final TeacherID lectureTeacher = new TeacherID(20);
    private static final DeanID dean = new DeanID(30);
    private static final GradingScale scale = new GradingScale(85, 70, 55);

    @Test
    void cannotOpenForInProgressCourse() {
        Course course = course();

        assertThatThrownBy(() -> new ControlSheet(course, ControlFormEnum.EXAM, scale))
                .isInstanceOf(CourseNotCompletedException.class);
    }

    @Test
    void examStoresNumericResultAndCalculatesFinalGrade() {
        Course course = completedCourseWithRating(80);
        ControlSheet sheet = new ControlSheet(course, ControlFormEnum.EXAM, scale);

        sheet.enterNumericResult(seminarTeacher, student, 70);
        sheet.close(seminarTeacher);

        assertThat(sheet.finalScore(student)).isEqualTo(75.0);
        assertThat(sheet.finalGrade(student)).isEqualTo(4);
    }

    @Test
    void creditStoresPassFailResult() {
        Course course = completedCourseWithRating(80);
        ControlSheet sheet = new ControlSheet(course, ControlFormEnum.CREDIT, scale);

        sheet.enterPassFailResult(seminarTeacher, student, PassFail.PASS);

        assertThat(sheet.resultOf(student).passFail()).isEqualTo(PassFail.PASS);
    }

    @Test
    void controlFormRejectsWrongResultType() {
        Course course = completedCourseWithRating(80);
        ControlSheet sheet = new ControlSheet(course, ControlFormEnum.DIFF_CREDIT, scale);

        assertThatThrownBy(() -> sheet.enterPassFailResult(seminarTeacher, student, PassFail.PASS))
                .isInstanceOf(InvalidControlResultException.class);
    }

    @Test
    void onlySeminarTeacherCanEnterResult() {
        Course course = completedCourseWithRating(80);
        ControlSheet sheet = new ControlSheet(course, ControlFormEnum.EXAM, scale);

        assertThatThrownBy(() -> sheet.enterNumericResult(lectureTeacher, student, 70))
                .isInstanceOf(UnauthorizedTeacherException.class);
    }

    @Test
    void cannotCloseUntilEveryStudentHasResult() {
        Course course = new Course(List.of(student, new StudentID(2)), seminarTeacher, lectureTeacher);
        course.addWork("Лабораторная", 100, LocalDate.of(2026, 9, 12));
        course.complete();
        ControlSheet sheet = new ControlSheet(course, ControlFormEnum.EXAM, scale);
        sheet.enterNumericResult(seminarTeacher, student, 70);

        assertThatThrownBy(() -> sheet.close(seminarTeacher))
                .isInstanceOf(IncompleteControlSheetException.class);
    }

    @Test
    void onlySeminarTeacherCanCloseSheet() {
        Course course = completedCourseWithRating(80);
        ControlSheet sheet = new ControlSheet(course, ControlFormEnum.EXAM, scale);
        sheet.enterNumericResult(seminarTeacher, student, 70);

        assertThatThrownBy(() -> sheet.close(lectureTeacher))
                .isInstanceOf(UnauthorizedTeacherException.class);
    }

    @Test
    void closedSheetRejectsNewResult() {
        Course course = completedCourseWithRating(80);
        ControlSheet sheet = new ControlSheet(course, ControlFormEnum.EXAM, scale);
        sheet.enterNumericResult(seminarTeacher, student, 70);
        sheet.close(seminarTeacher);

        assertThatThrownBy(() -> sheet.enterNumericResult(seminarTeacher, student, 90))
                .isInstanceOf(ControlSheetException.class);
    }

    @Test
    void recordsInitialResult() {
        ControlSheet sheet = new ControlSheet(
                completedCourseWithRating(80),
                ControlFormEnum.EXAM,
                scale
        );

        sheet.enterNumericResult(seminarTeacher, student, 70);

        assertThat(sheet.events()).hasSize(1);
        ControlSheetEvent event = sheet.events().getFirst();
        assertThat(event.eventType()).isEqualTo(ControlEventType.RESULT_ENTERED);
        assertThat(event.actorType()).isEqualTo(ControlActorType.TEACHER);
        assertThat(event.actorId()).isEqualTo(seminarTeacher.id());
        assertThat(event.studentID()).isEqualTo(student);
        assertThat(event.oldResult()).isNull();
        assertThat(event.newResult()).isEqualTo(ControlResult.numeric(70));
        assertThat(event.createdAt()).isNotNull();
    }

    @Test
    void recordsLifecycleAndChangedResultAfterReopening() {
        ControlSheet sheet = new ControlSheet(
                completedCourseWithRating(80),
                ControlFormEnum.EXAM,
                scale
        );
        sheet.enterNumericResult(seminarTeacher, student, 70);
        sheet.close(seminarTeacher);

        sheet.reopen(dean, "Пересдача");
        sheet.enterNumericResult(seminarTeacher, student, 90, "Результат пересдачи");
        sheet.close(seminarTeacher);

        assertThat(sheet.status()).isEqualTo(ControlSheetStatus.CLOSED);
        assertThat(sheet.resultOf(student)).isEqualTo(ControlResult.numeric(90));
        assertThat(sheet.events())
                .extracting(ControlSheetEvent::eventType)
                .containsExactly(
                        ControlEventType.RESULT_ENTERED,
                        ControlEventType.CLOSED,
                        ControlEventType.REOPENED,
                        ControlEventType.RESULT_CHANGED,
                        ControlEventType.CLOSED
                );

        ControlSheetEvent changed = sheet.events().get(3);
        assertThat(changed.oldResult()).isEqualTo(ControlResult.numeric(70));
        assertThat(changed.newResult()).isEqualTo(ControlResult.numeric(90));
        assertThat(changed.reason()).isEqualTo("Результат пересдачи");
    }

    @Test
    void deanArchivesClosedSheet() {
        ControlSheet sheet = new ControlSheet(
                completedCourseWithRating(80),
                ControlFormEnum.EXAM,
                scale
        );
        sheet.enterNumericResult(seminarTeacher, student, 70);
        sheet.close(seminarTeacher);

        sheet.archive(dean, "Сессия завершена");

        assertThat(sheet.status()).isEqualTo(ControlSheetStatus.ARCHIVED);
        assertThat(sheet.finalGrade(student)).isEqualTo(4);
        assertThat(sheet.events().getLast().eventType()).isEqualTo(ControlEventType.ARCHIVED);
        assertThat(sheet.events().getLast().actorId()).isEqualTo(dean.id());
    }

    @Test
    void rejectsInvalidDeanTransitions() {
        ControlSheet sheet = new ControlSheet(
                completedCourseWithRating(80),
                ControlFormEnum.EXAM,
                scale
        );

        assertThatThrownBy(() -> sheet.reopen(dean))
                .isInstanceOf(SheetStatusException.class);
        assertThatThrownBy(() -> sheet.archive(dean))
                .isInstanceOf(SheetStatusException.class);

        sheet.enterNumericResult(seminarTeacher, student, 70);
        sheet.close(seminarTeacher);
        sheet.archive(dean);

        assertThatThrownBy(() -> sheet.reopen(dean))
                .isInstanceOf(SheetStatusException.class);
        assertThatThrownBy(() -> sheet.enterNumericResult(seminarTeacher, student, 90))
                .isInstanceOf(ControlSheetException.class);
    }

    private Course course() {
        Course course = new Course(List.of(student), seminarTeacher, lectureTeacher);
        course.addWork("Лабораторная", 100, LocalDate.of(2026, 9, 12));
        return course;
    }

    private Course completedCourseWithRating(int score) {
        Course course = course();
        long work = 1;
        course.score(work, student, score);
        course.complete();
        return course;
    }
}
