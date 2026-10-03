package com.dev.csas.course;

import com.dev.csas.shared.StudentID;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CourseTest {
    private static final StudentID student = new StudentID(1);

    private long addWork(Course course, String title, int maxScore) {
        return course.addWork(title, maxScore, LocalDate.of(2026, 9, 12));
    }

    @Test
    void ratingEqualsToRatioOfPointsScoredToMaximumPossible() {
        Course course = new Course(List.of(student), CourseType.SEMINAR);
        long work1 = addWork(course, "Лабораторная 1", 20);
        long work2 = addWork(course, "Лабораторная 2", 30);

        course.score(work1, student, 18);
        course.score(work2, student, 15);

        assertThat(course.rating(student)).isEqualTo(66.0);
    }

    @Test
    void updateWorkChangesRating() {
        Course course = new Course(List.of(student), CourseType.SEMINAR);
        long work1 = addWork(course, "Лабораторная 1", 20);
        course.score(work1, student, 15);
        double ratingBefore = course.rating(student);

        course.updateWorkMaxScore(work1, 25);
        double ratingAfter = course.rating(student);

        assertThat(ratingBefore).isEqualTo(75.0);
        assertThat(ratingAfter).isEqualTo(60.0);
    }

    @Test
    void updateScoreReplacesPrevious() {
        int scoreDiff = 5;
        int initialScore = 10;

        Course course = new Course(List.of(student), CourseType.SEMINAR);
        long work1 = addWork(course, "Лабораторная 1", 20);

        course.score(work1, student, initialScore);
        course.score(work1, student, initialScore + scoreDiff);

        assertThat(course.rating(student)).isEqualTo(75.0);
    }

    @Test
    void tryToSetScoreToNonExistentStudentWillThrowAnException() {
        Course course = new Course(List.of(), CourseType.SEMINAR);
        long work1 = addWork(course, "Лабораторная 1", 20);

        assertThatThrownBy(() -> course.score(work1, student, 20))
            .isInstanceOf(NonExistentStudentException.class);
    }

    @Test
    void tryToGetRatingOfNonExistentStudentWillThrowAnException() {
        Course course = new Course(List.of(), CourseType.SEMINAR);
        long work1 = addWork(course, "Лабораторная 1", 20);

        assertThatThrownBy(() -> course.rating(student))
                .isInstanceOf(NonExistentStudentException.class);
    }

    @Test
    void ratingWithoutWorksAndScoresShouldReturn100() {
        Course course = new Course(List.of(student), CourseType.SEMINAR);

        assertThat(course.rating(student)).isEqualTo(100.0);
    }

    @Test
    void unableToCreateWorkWithMaxScoreEqual0() {
        Course course = new Course(List.of(student), CourseType.SEMINAR);
        assertThatThrownBy(() -> course.addWork("work", 0, LocalDate.of(2026, 9, 12)))
                .isInstanceOf(InvalidWorkMaxScoreException.class);
    }

    @Test
    void unableToUpdateWorkWithMaxScoreEqual0() {
        Course course = new Course(List.of(student), CourseType.SEMINAR);
        long workNum = addWork(course, "Лабораторная 1", 20);

        assertThatThrownBy(() -> course.updateWorkMaxScore(workNum, 0))
                .isInstanceOf(InvalidWorkMaxScoreException.class);
    }

    @Test
    void cannotLowerWorkMaximumBelowAnExistingScore() {
        Course course = new Course(List.of(student), CourseType.SEMINAR);
        long workNum = addWork(course, "Лабораторная 1", 20);
        course.score(workNum, student, 15);

        assertThatThrownBy(() -> course.updateWorkMaxScore(workNum, 10))
                .isInstanceOf(InvalidWorkMaxScoreException.class);
    }

    @Test
    void unableToSetScoreWithValueGreaterThenWorkMaxScore() {
        Course course = new Course(List.of(student), CourseType.SEMINAR);
        long workNum = addWork(course, "Лабораторная 1", 20);

        assertThatThrownBy(() -> course.score(workNum, student, 21))
                .isInstanceOf(InvalidScoreValueException.class);
    }

    @Test
    void addWorkStoresDate() {
        Course course = new Course(List.of(student), CourseType.SEMINAR);
        LocalDate date = LocalDate.of(2026, 9, 12);

        long workNumber = course.addWork("Лабораторная 1", 20, date);

        assertThat(course.getWorkDate(workNumber)).isEqualTo(date);
    }

    @Test
    void updateWorkDateChangesDateOfThisWork() {
        Course course = new Course(List.of(student), CourseType.SEMINAR);
        LocalDate date = LocalDate.of(2026, 9, 12);
        LocalDate newDate = LocalDate.of(2026, 9, 18);
        long workNumber = course.addWork("Лабораторная 1", 20, date);

        LocalDate otherDate = LocalDate.of(2026, 9, 12);
        long otherWorkNumber = course.addWork("Лабораторная 1", 20, otherDate);


        course.updateWorkDate(workNumber, newDate);

        assertThat(course.getWorkDate(workNumber)).isEqualTo(newDate);
        assertThat(course.getWorkDate(otherWorkNumber)).isEqualTo(otherDate);
    }

    @Test
    void updateNonExistingWorkWillThrowWorkNotFoundException() {
        Course course = new Course(List.of(student), CourseType.SEMINAR);
        LocalDate newDate = LocalDate.of(2026, 9, 18);

        assertThatThrownBy(() -> course.updateWorkDate(40, newDate))
                .isInstanceOf(WorkNotFoundException.class);
    }

    @Test
    void unableToCreateWorkWithNullDate() {
        Course course = new Course(List.of(student), CourseType.SEMINAR);

        assertThatThrownBy(() -> course.addWork("Лабораторная 1", 20, null))
                .isInstanceOf(InvalidWorkDateException.class);
    }

    @Test
    void unableToUpdateWorkWithNullNewDate() {
        Course course = new Course(List.of(student), CourseType.SEMINAR);

        LocalDate date = LocalDate.of(2026, 9, 12);
        long workNumber = course.addWork("Лабораторная 1", 20, date);

        assertThatThrownBy(() -> course.updateWorkDate(workNumber, null))
                .isInstanceOf(InvalidWorkDateException.class);
    }

    @Test
    void initialScoreShouldBe0() {
        StudentID secondStudent = new StudentID(2);
        Course course = new Course(List.of(student, secondStudent), CourseType.SEMINAR);
        LocalDate date = LocalDate.of(2026, 9, 12);
        long work = course.addWork("Лабораторная 1", 20, date);

        assertThat(course.getScore(work, student)).isEqualTo(0);
        assertThat(course.getScore(work, secondStudent)).isEqualTo(0);
    }

    @Test
    void lectureCourseCannotCreateSeminarWork() {
        Course course = new Course(List.of(student), CourseType.LECTURE);

        assertThatThrownBy(() -> course.addWork("Работа", 20, LocalDate.of(2026, 9, 12)))
                .isInstanceOf(InvalidCourseTypeException.class);
    }

    @Test
    void courseworkAndLectureCoursesCanBeCompletedWithoutSeminarWorks() {
        Course lecture = new Course(List.of(student), CourseType.LECTURE);
        Course coursework = new Course(List.of(student), CourseType.COURSEWORK);

        lecture.complete();
        coursework.complete();

        assertThat(lecture.isCompleted()).isTrue();
        assertThat(coursework.isCompleted()).isTrue();
    }

}
