package com.dev.csas.control;

import com.dev.csas.course.Course;
import com.dev.csas.course.CourseNotCompletedException;
import com.dev.csas.discipline.ControlFormEnum;
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
