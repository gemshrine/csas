package com.dev.csas.course;

import com.dev.csas.student.StudentID;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CourseTest {
    private static final StudentID student = new StudentID(1);

    @Test
    void ratingEqualsToRatioOfPointsScoredToMaximumPossible() {
        Course course = new Course(List.of(student));
        long work1 = course.addWork("Лабораторная 1", 20);
        long work2 = course.addWork("Лабораторная 2", 30);

        course.score(work1, student, 18);
        course.score(work2, student, 15);

        assertThat(course.rating(student)).isEqualTo(66.0);
    }

    @Test
    void updateWorkChangesRating() {
        Course course = new Course(List.of(student));
        long work1 = course.addWork("Лабораторная 1", 20);
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

        Course course = new Course(List.of(student));
        long work1 = course.addWork("Лабораторная 1", 20);

        course.score(work1, student, initialScore);
        course.score(work1, student, initialScore + scoreDiff);

        assertThat(course.rating(student)).isEqualTo(75.0);
    }

    @Test
    void tryToSetScoreToNonExistentStudentWillThrowAnException() {
        Course course = new Course(List.of());
        long work1 = course.addWork("Лабораторная 1", 20);

        assertThatThrownBy(() -> course.score(work1, student, 20))
            .isInstanceOf(NonExistentStudentException.class);
    }

    @Test
    void tryToGetRatingOfNonExistentStudentWillThrowAnException() {
        Course course = new Course(List.of());
        course.addWork("Лабораторная 1", 20);

        assertThatThrownBy(() -> course.rating(student))
                .isInstanceOf(NonExistentStudentException.class);
    }

    @Test
    void ratingWithoutWorksAndScoresWillReturn100() {
        Course course = new Course(List.of(student));

        assertThat(course.rating(student)).isEqualTo(100.0);
    }

    @Test
    void unableToCreateWorkWithMaxScoreEqual0() {
        Course course = new Course(List.of(student));
        assertThatThrownBy(() -> course.addWork("work", 0))
                .isInstanceOf(InvalidWorkMaxScoreException.class);
    }

    @Test
    void unableToUpdateWorkWithMaxScoreEqual0() {
        Course course = new Course(List.of(student));
        long workNum = course.addWork("Лабораторная 1", 20);

        assertThatThrownBy(() -> course.updateWorkMaxScore(workNum, 0))
                .isInstanceOf(InvalidWorkMaxScoreException.class);
    }

    @Test
    void unableToSetScoreWithValueGreaterThenWorkMaxScore() {
        Course course = new Course(List.of(student));
        long workNum = course.addWork("Лабораторная 1", 20);

        assertThatThrownBy(() -> course.score(workNum, student, 21))
                .isInstanceOf(InvalidScoreValueException.class);
    }

}
