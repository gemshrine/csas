package com.dev.csas.control;

import com.dev.csas.course.Course;
import com.dev.csas.course.CourseNotCompletedException;
import com.dev.csas.course.NonExistentStudentException;
import com.dev.csas.discipline.ControlFormEnum;
import com.dev.csas.shared.StudentID;
import com.dev.csas.shared.TeacherID;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ControlSheet {

    private final Course course;
    private final ControlFormEnum controlForm;
    private final GradingScale gradingScale;
    private final Map<StudentID, ControlResult> results = new HashMap<>();
    private ControlSheetStatus status = ControlSheetStatus.OPEN;

    public ControlSheet(Course course, ControlFormEnum controlForm, GradingScale gradingScale) {
        if (!course.isCompleted()) {
            throw new CourseNotCompletedException("Нельзя открыть ведомость для незавершённого курса");
        }
        this.course = Objects.requireNonNull(course);
        this.controlForm = Objects.requireNonNull(controlForm);
        this.gradingScale = Objects.requireNonNull(gradingScale);
    }

    public void enterNumericResult(TeacherID teacher, StudentID student, int score) {
        validateEntry(teacher, student);
        if (controlForm == ControlFormEnum.CREDIT) {
            throw new InvalidControlResultException("Для обычного зачёта нужен результат зачёт/незачёт");
        }
        if (score < 0 || score > 100) {
            throw new InvalidControlResultException("Результат контроля должен быть от 0 до 100");
        }
        results.put(student, ControlResult.numeric(score));
    }

    public void enterPassFailResult(TeacherID teacher, StudentID student, PassFail result) {
        validateEntry(teacher, student);
        if (controlForm != ControlFormEnum.CREDIT) {
            throw new InvalidControlResultException("Для экзамена и диф. зачёта нужен числовой результат");
        }
        results.put(student, ControlResult.passFail(Objects.requireNonNull(result)));
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
        if (status != ControlSheetStatus.CLOSED) {
            throw new ControlSheetException("Итоговую оценку можно получить только из закрытой ведомости");
        }
        return gradingScale.grade(finalScore(student));
    }

    public void close(TeacherID teacher) {
        validateSeminarTeacher(teacher);

        List<StudentID> students = course.students();
        if (!results.keySet().containsAll(students)) {
            throw new IncompleteControlSheetException("Нельзя закрыть ведомость без результатов всех студентов");
        }
        status = ControlSheetStatus.CLOSED;
    }

    public ControlSheetStatus status() {
        return status;
    }

    private void validateEntry(TeacherID teacher, StudentID student) {
        if (status != ControlSheetStatus.OPEN) {
            throw new ControlSheetException("Ведомость закрыта или архивирована");
        }
        if (!teacher.equals(course.seminarTeacher())) {
            throw new UnauthorizedTeacherException("Результат может внести только преподаватель семинарной части");
        }
        validateStudent(student);
    }

    private void validateStudent(StudentID student) {
        if (!course.hasStudent(student)) {
            throw new NonExistentStudentException("Студент не относится к курсу: " + student.id());
        }
    }

    private void validateSeminarTeacher(TeacherID teacher) {
        if (!teacher.equals(course.seminarTeacher())) {
            throw new UnauthorizedTeacherException(
                    "Операцию может выполнить только преподаватель семинарной части"
            );
        }
    }
}
