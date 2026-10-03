package com.dev.csas.coursework;

import com.dev.csas.course.Course;
import com.dev.csas.course.CourseNotCompletedException;
import com.dev.csas.course.CourseType;
import com.dev.csas.course.InvalidCourseTypeException;
import com.dev.csas.security.ActorAccessDeniedException;
import com.dev.csas.shared.DeanID;
import com.dev.csas.shared.StudentID;
import com.dev.csas.shared.TeacherID;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CourseworkSheet {

    private final Course course;
    private final Map<StudentID, Integer> scores = new HashMap<>();
    private CourseworkSheetStatus status = CourseworkSheetStatus.OPEN;

    public CourseworkSheet(Course course) {
        this.course = Objects.requireNonNull(course);
        if (course.type() != CourseType.COURSEWORK) {
            throw new InvalidCourseTypeException("Ведомость курсовых работ относится только к курсовому курсу");
        }
    }

    static CourseworkSheet reconstitute(
            Course course,
            CourseworkSheetStatus status,
            Map<StudentID, Integer> scores
    ) {
        CourseworkSheet sheet = new CourseworkSheet(course);
        sheet.status = Objects.requireNonNull(status);
        sheet.scores.putAll(scores);
        return sheet;
    }

    public void enterScore(TeacherID teacher, StudentID student, int score) {
        validateOpen();
        validateTeacher(teacher);
        validateStudent(student);

        if (score < 0 || score > 100) {
            throw new InvalidCourseworkScoreException("Балл за курсовую работу должен быть от 0 до 100");
        }

        scores.put(student, score);
    }

    public CourseworkGrade gradeOf(StudentID student) {
        return gradeOfScore(scoreOf(student));
    }

    public int scoreOf(StudentID student) {
        validateStudent(student);
        Integer score = scores.get(student);
        if (score == null) {
            throw new CourseworkResultNotFoundException("Балл за курсовую работу ещё не внесён");
        }
        return score;
    }

    public CourseworkGrade gradeOfScore(int score) {
        return CourseworkGrade.fromScore(score);
    }

    public void close(TeacherID teacher) {
        validateOpen();
        validateTeacher(teacher);
        if (!course.isCompleted()) {
            throw new CourseNotCompletedException("Курсовой курс должен быть завершён до закрытия ведомости");
        }
        if (!scores.keySet().containsAll(course.students())) {
            throw new IncompleteCourseworkSheetException(
                    "Нельзя закрыть ведомость без баллов всех студентов курса"
            );
        }
        status = CourseworkSheetStatus.CLOSED;
    }

    public void reopen(DeanID dean) {
        Objects.requireNonNull(dean, "Деканат обязателен");
        if (status != CourseworkSheetStatus.CLOSED) {
            throw new CourseworkSheetStatusException("Переоткрыть можно только закрытую ведомость");
        }
        status = CourseworkSheetStatus.OPEN;
    }

    public void archive(DeanID dean) {
        Objects.requireNonNull(dean, "Деканат обязателен");
        if (status != CourseworkSheetStatus.CLOSED) {
            throw new CourseworkSheetStatusException("Архивировать можно только закрытую ведомость");
        }
        status = CourseworkSheetStatus.ARCHIVED;
    }

    public CourseworkSheetStatus status() {
        return status;
    }

    public Map<StudentID, Integer> scores() {
        return Map.copyOf(scores);
    }

    public Course course() {
        return course;
    }

    private void validateOpen() {
        if (status != CourseworkSheetStatus.OPEN) {
            throw new CourseworkSheetStatusException("Ведомость закрыта или архивирована");
        }
    }

    private void validateTeacher(TeacherID teacher) {
        if (!Objects.requireNonNull(teacher).equals(course.teacher())) {
            throw new ActorAccessDeniedException("Результат может внести только преподаватель курсового курса");
        }
    }

    private void validateStudent(StudentID student) {
        if (!course.hasStudent(Objects.requireNonNull(student))) {
            throw new CourseworkSheetStudentNotFoundException("Студент не относится к курсовому курсу");
        }
    }
}
