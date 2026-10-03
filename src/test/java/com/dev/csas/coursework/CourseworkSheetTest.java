package com.dev.csas.coursework;

import com.dev.csas.course.Course;
import com.dev.csas.course.CourseType;
import com.dev.csas.course.InvalidCourseTypeException;
import com.dev.csas.security.ActorAccessDeniedException;
import com.dev.csas.shared.DeanID;
import com.dev.csas.shared.StudentID;
import com.dev.csas.shared.TeacherID;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseworkSheetTest {

    private final StudentID firstStudent = new StudentID(1);
    private final StudentID secondStudent = new StudentID(2);
    private final TeacherID teacher = new TeacherID(10);
    private final DeanID dean = new DeanID(20);

    @Test
    void mapsScoresToTheAgreedGradeBands() {
        CourseworkSheet sheet = openSheet(firstStudent);

        sheet.enterScore(teacher, firstStudent, 85);
        assertThat(sheet.gradeOf(firstStudent)).isEqualTo(CourseworkGrade.FIVE);

        sheet.enterScore(teacher, firstStudent, 70);
        assertThat(sheet.gradeOf(firstStudent)).isEqualTo(CourseworkGrade.FOUR);
        sheet.enterScore(teacher, firstStudent, 84);
        assertThat(sheet.gradeOf(firstStudent)).isEqualTo(CourseworkGrade.FOUR);

        sheet.enterScore(teacher, firstStudent, 50);
        assertThat(sheet.gradeOf(firstStudent)).isEqualTo(CourseworkGrade.THREE);
        sheet.enterScore(teacher, firstStudent, 69);
        assertThat(sheet.gradeOf(firstStudent)).isEqualTo(CourseworkGrade.THREE);

        sheet.enterScore(teacher, firstStudent, 49);
        assertThat(sheet.gradeOf(firstStudent)).isEqualTo(CourseworkGrade.NOT_PASSED);
    }

    @Test
    void rejectsScoresOutsideZeroToOneHundred() {
        CourseworkSheet sheet = openSheet(firstStudent);

        assertThatThrownBy(() -> sheet.enterScore(teacher, firstStudent, -1))
                .isInstanceOf(InvalidCourseworkScoreException.class);
        assertThatThrownBy(() -> sheet.enterScore(teacher, firstStudent, 101))
                .isInstanceOf(InvalidCourseworkScoreException.class);
    }

    @Test
    void onlyAssignedTeacherCanEnterScore() {
        CourseworkSheet sheet = openSheet(firstStudent);

        assertThatThrownBy(() -> sheet.enterScore(new TeacherID(11), firstStudent, 80))
                .isInstanceOf(ActorAccessDeniedException.class);
    }

    @Test
    void onlyCourseStudentsCanReceiveScore() {
        CourseworkSheet sheet = openSheet(firstStudent);

        assertThatThrownBy(() -> sheet.enterScore(teacher, secondStudent, 80))
                .isInstanceOf(CourseworkSheetStudentNotFoundException.class);
    }

    @Test
    void cannotCloseUntilEveryStudentHasAScore() {
        CourseworkSheet sheet = openSheet(firstStudent, secondStudent);
        sheet.enterScore(teacher, firstStudent, 80);
        sheet.course().complete();

        assertThatThrownBy(() -> sheet.close(teacher))
                .isInstanceOf(IncompleteCourseworkSheetException.class);
    }

    @Test
    void cannotCloseBeforeCourseIsCompleted() {
        CourseworkSheet sheet = openSheet(firstStudent);
        sheet.enterScore(teacher, firstStudent, 80);

        assertThatThrownBy(() -> sheet.close(teacher))
                .isInstanceOf(com.dev.csas.course.CourseNotCompletedException.class);
    }

    @Test
    void closedSheetCanBeReopenedByDeanForRetake() {
        CourseworkSheet sheet = openSheet(firstStudent);
        sheet.enterScore(teacher, firstStudent, 80);
        sheet.course().complete();
        sheet.close(teacher);

        sheet.reopen(dean);

        assertThat(sheet.status()).isEqualTo(CourseworkSheetStatus.OPEN);
    }

    @Test
    void closedSheetCannotBeChangedUntilDeanReopensIt() {
        CourseworkSheet sheet = openSheet(firstStudent);
        sheet.enterScore(teacher, firstStudent, 80);
        sheet.course().complete();
        sheet.close(teacher);

        assertThatThrownBy(() -> sheet.enterScore(teacher, firstStudent, 90))
                .isInstanceOf(CourseworkSheetStatusException.class);
    }

    @Test
    void cannotReopenSheetThatIsNotClosed() {
        CourseworkSheet sheet = openSheet(firstStudent);

        assertThatThrownBy(() -> sheet.reopen(dean))
                .isInstanceOf(CourseworkSheetStatusException.class);
    }

    @Test
    void archivesClosedSheet() {
        CourseworkSheet sheet = openSheet(firstStudent);
        sheet.enterScore(teacher, firstStudent, 80);
        sheet.course().complete();
        sheet.close(teacher);

        sheet.archive(dean);

        assertThat(sheet.status()).isEqualTo(CourseworkSheetStatus.ARCHIVED);
    }

    @Test
    void onlyCourseworkCoursesCanHaveCourseworkSheets() {
        Course course = new Course(List.of(firstStudent), CourseType.SEMINAR, teacher);

        assertThatThrownBy(() -> new CourseworkSheet(course))
                .isInstanceOf(InvalidCourseTypeException.class);
    }

    private CourseworkSheet openSheet(StudentID... students) {
        Course course = new Course(List.of(students), CourseType.COURSEWORK, teacher);
        return new CourseworkSheet(course);
    }
}
