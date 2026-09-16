package com.dev.csas.attendance;

import com.dev.csas.control.UnauthorizedTeacherException;
import com.dev.csas.course.Course;
import com.dev.csas.course.CourseCompletedException;
import com.dev.csas.course.NonExistentStudentException;
import com.dev.csas.shared.LectureID;
import com.dev.csas.shared.StudentID;
import com.dev.csas.shared.TeacherID;

import java.time.LocalDate;
import java.util.*;

public class AttendanceSheet {
    private final Course course;
    private final Map<LectureID, Map<StudentID, AttendanceRecord>> attendances = new HashMap<>();
    private final List<Lecture> lectures = new ArrayList<>();
    private AttendanceSheetStatus status = AttendanceSheetStatus.OPEN;

    public AttendanceSheet(Course course) {
        if (course.isCompleted()) {
            throw new CourseCompletedException("Курс не должен быть завершенным");
        }
        this.course = course;
    }

    public void addLecture(TeacherID teacherID, LectureID lectureID, LocalDate date, String title) {

        if (findLecture(lectureID) != null) {
            throw new DuplicateLectureException("Лекция уже существует");
        }

        validateOpen();
        validateTeacher(teacherID);

        lectures.add(new Lecture(lectureID, date, title));
    }

    public void register(TeacherID teacherID, StudentID student, AttendanceStatusEnum status, LectureID lectureID) {
        validateOpen();
        validateTeacher(teacherID);

        if (findLecture(lectureID) == null) {
            throw new LectureNotFoundException("Лекция не найдена");
        }

        if (!course.hasStudent(student)) {
            throw new NonExistentStudentException("Студент не относится к курсу");
        }

        Map<StudentID, AttendanceRecord> lectureAttendance =
                attendances.computeIfAbsent(
                        lectureID,
                        k -> new HashMap<>()
                );

        lectureAttendance.put(student, new AttendanceRecord(status));
    }

    public void close(TeacherID teacherID) {
        validateTeacher(teacherID);
        validateAllStudentsRegistered();
        this.status = AttendanceSheetStatus.CLOSED;
    }

    public AttendanceSheetStatus status() {
        return status;
    }

    public Map<StudentID, AttendanceRecord> getAttendances(LectureID lectureID) {
        return Collections.unmodifiableMap(attendances.get(lectureID));
    }

    private Lecture findLecture(LectureID lectureID) {
        return lectures.stream()
                .filter(lecture -> lecture.getId().equals(lectureID))
                .findFirst()
                .orElse(null);
    }

    private void validateTeacher(TeacherID teacherID) {
        if (!teacherID.equals(course.lectureTeacher())) {
            throw new UnauthorizedTeacherException("Преподаватель не является лектором");
        }
    }

    private void validateOpen() {
        if (status != AttendanceSheetStatus.OPEN || course.isCompleted()) {
            throw new AttendanceSheetClosedException("Нельзя вносить измнения вносить изменения в закрытую ведомость");
        }
    }

    private void validateAllStudentsRegistered() {
        for (Lecture lecture : lectures) {
            LectureID id = lecture.getId();
            Map<StudentID, AttendanceRecord> lectureAttendance = attendances.get(id);

            if (lectureAttendance == null || !lectureAttendance.keySet().containsAll(course.students())) {
                throw new InvalidRegisterException(
                        String.format("Не все студенты отмечены на лекции: %s", id)
                );
            }
        }
    }

}
