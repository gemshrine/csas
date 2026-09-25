package com.dev.csas.attendance;

import com.dev.csas.control.UnauthorizedTeacherException;
import com.dev.csas.course.Course;
import com.dev.csas.course.CourseCompletedException;
import com.dev.csas.course.NonExistentStudentException;
import com.dev.csas.shared.*;

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

    private AttendanceSheet(Course course, AttendanceSheetStatus status) {
        this.course = Objects.requireNonNull(course);
        this.status = Objects.requireNonNull(status);
    }

    static AttendanceSheet reconstitute(
            Course course,
            AttendanceSheetStatus status,
            List<Lecture> lectures,
            Map<LectureID, Map<StudentID, AttendanceRecord>> attendances
    ) {
        AttendanceSheet sheet = new AttendanceSheet(course, status);
        sheet.lectures.addAll(lectures);
        attendances.forEach((lectureID, records) -> sheet.attendances.put(lectureID, new HashMap<>(records)));
        return sheet;
    }

    public void addLecture(TeacherID teacherID, LectureID lectureID, LocalDate date, String title) {
        validateOpen();
        validateTeacher(teacherID);

        if (findLecture(lectureID) != null) {
            throw new DuplicateLectureException("Лекция уже существует");
        }

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
        validateOpen();
        validateAllStudentsRegistered();

        this.status = AttendanceSheetStatus.CLOSED;
    }

    public void archive(DeanID dean) {
        Objects.requireNonNull(dean, "Деканат обязателен");

        if (status != AttendanceSheetStatus.CLOSED) {
            throw new SheetStatusException("Архивировать можно только закрытую ведомость");
        }

        this.status = AttendanceSheetStatus.ARCHIVED;
    }

    public AttendanceSheetStatus status() {
        return status;
    }

    public List<Lecture> lectures() {
        return List.copyOf(lectures);
    }

    public Map<LectureID, Map<StudentID, AttendanceRecord>> attendances() {
        Map<LectureID, Map<StudentID, AttendanceRecord>> result = new HashMap<>();
        attendances.forEach((lectureID, records) -> result.put(lectureID, Map.copyOf(records)));
        return Map.copyOf(result);
    }

    public Map<StudentID, AttendanceRecord> getAttendances(LectureID lectureID) {
        if (findLecture(lectureID) == null) {
            throw new LectureNotFoundException(String.format("Лекция не найдена: %s", lectureID));
        }

        return Map.copyOf(attendances.getOrDefault(lectureID,  Collections.emptyMap()));
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
            throw new SheetStatusException("Нельзя вносить изменения в закрытую ведомость");
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
