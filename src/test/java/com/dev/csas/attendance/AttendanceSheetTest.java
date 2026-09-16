package com.dev.csas.attendance;

import com.dev.csas.control.UnauthorizedTeacherException;
import com.dev.csas.course.Course;
import com.dev.csas.course.NonExistentStudentException;
import com.dev.csas.shared.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class AttendanceSheetTest {
    private static final StudentID student = new StudentID(1);
    private static final StudentID otherStudent = new StudentID(20);
    private static final TeacherID seminarTeacher = new TeacherID(10);
    private static final TeacherID lectureTeacher = new TeacherID(20);
    private static final DeanID dean = new DeanID(1);

    @Test
    void addStudentDoesNotRewriteExisting() {
        Course course = new Course(List.of(student, otherStudent), seminarTeacher, lectureTeacher);
        AttendanceSheet attendanceSheet = new AttendanceSheet(course);
        LectureID lectureID = new LectureID(1);
        attendanceSheet.addLecture(lectureTeacher, lectureID, LocalDate.of(2026, 9, 14), "Лекция 1");

        attendanceSheet.register(lectureTeacher, student, AttendanceStatusEnum.ABSENT, lectureID);
        attendanceSheet.register(lectureTeacher, otherStudent, AttendanceStatusEnum.ABSENT, lectureID);

        assertThat(attendanceSheet.getAttendances(lectureID).size()).isEqualTo(2);
    }

    @Test
    void seminarTeacherCannotAddLecture() {
        Course course = new Course(List.of(student, otherStudent), seminarTeacher, lectureTeacher);
        AttendanceSheet attendanceSheet = new AttendanceSheet(course);
        LectureID lectureID = new LectureID(1);

        assertThatThrownBy(() -> attendanceSheet.addLecture(seminarTeacher, lectureID, LocalDate.of(2026, 9, 14), "Лекция 1"))
                .isInstanceOf(UnauthorizedTeacherException.class);
    }

    @Test
    void unableToRegisterNotBelongingStudent() {
        Course course = new Course(List.of(student), seminarTeacher, lectureTeacher);
        AttendanceSheet attendanceSheet = new AttendanceSheet(course);
        LectureID lectureID = new LectureID(1);
        attendanceSheet.addLecture(lectureTeacher, lectureID, LocalDate.of(2026, 9, 14), "Лекция 1");

        assertThatThrownBy(() -> attendanceSheet.register(lectureTeacher, otherStudent, AttendanceStatusEnum.ABSENT, lectureID))
            .isInstanceOf(NonExistentStudentException.class);
    }

    @Test
    void cannotCloseUntilAllStudentsAreRegistered() {
        Course course = new Course(List.of(student, otherStudent), seminarTeacher, lectureTeacher);
        AttendanceSheet attendanceSheet = new AttendanceSheet(course);
        LectureID lectureID = new LectureID(1);
        attendanceSheet.addLecture(lectureTeacher, lectureID, LocalDate.of(2026, 9, 14), "Лекция 1");
        attendanceSheet.register(lectureTeacher, student, AttendanceStatusEnum.PRESENT, lectureID);

        assertThatThrownBy(() -> attendanceSheet.close(lectureTeacher))
                .isInstanceOf(InvalidRegisterException.class);

        attendanceSheet.register(lectureTeacher, otherStudent, AttendanceStatusEnum.ABSENT, lectureID);
        attendanceSheet.close(lectureTeacher);

        assertThat(attendanceSheet.status()).isEqualTo(AttendanceSheetStatus.CLOSED);
    }

    @Test
    void cannotArchiveOpenedSheet() {
        Course course = new Course(List.of(student, otherStudent), seminarTeacher, lectureTeacher);
        AttendanceSheet attendanceSheet = new AttendanceSheet(course);

        assertThatThrownBy(() -> attendanceSheet.archive(dean))
            .isInstanceOf(SheetStatusException.class);
    }


    @Test
    void cannotArchiveArchivedSheet() {
        Course course = new Course(List.of(student, otherStudent), seminarTeacher, lectureTeacher);
        AttendanceSheet attendanceSheet = new AttendanceSheet(course);
        attendanceSheet.close(lectureTeacher);
        attendanceSheet.archive(dean);

        assertThatThrownBy(() -> attendanceSheet.archive(dean))
                .isInstanceOf(SheetStatusException.class);
    }

    @Test
    void cannotActInSheetAfterArchivingOrClosing() {
        Course course1 = new Course(List.of(student, otherStudent), seminarTeacher, lectureTeacher);
        Course course2 = new Course(List.of(student, otherStudent), seminarTeacher, lectureTeacher);

        AttendanceSheet attendanceSheet1 = new AttendanceSheet(course1);
        AttendanceSheet attendanceSheet2 = new AttendanceSheet(course2);

        LectureID lectureID1 = new LectureID(1);
        LectureID lectureID2 = new LectureID(2);

        attendanceSheet1.close(lectureTeacher);
        attendanceSheet2.close(lectureTeacher);
        attendanceSheet2.archive(dean);

        assertThatThrownBy(() -> attendanceSheet1.addLecture(lectureTeacher, lectureID1, LocalDate.of(2026, 9, 14), "lab1"))
                .isInstanceOf(SheetStatusException.class);
        assertThatThrownBy(() -> attendanceSheet2.addLecture(lectureTeacher, lectureID2, LocalDate.of(2026, 9, 14), "kt1"))
                .isInstanceOf(SheetStatusException.class);
        assertThatThrownBy(() -> attendanceSheet1.register(lectureTeacher, student, AttendanceStatusEnum.ABSENT, lectureID1))
                .isInstanceOf(SheetStatusException.class);
        assertThatThrownBy(() -> attendanceSheet2.register(lectureTeacher, student, AttendanceStatusEnum.ABSENT, lectureID1))
                .isInstanceOf(SheetStatusException.class);

    }

    @Test
    void rejectsReadingUnknownLecture() {
        Course course = new Course(List.of(student, otherStudent), seminarTeacher, lectureTeacher);
        AttendanceSheet attendanceSheet = new AttendanceSheet(course);
        LectureID lectureID = new LectureID(1);

        assertThatThrownBy(() -> attendanceSheet.getAttendances(lectureID))
                .isInstanceOf(LectureNotFoundException.class);
    }

    @Test
    void existingLectureWithoutRecordsReturnsEmptyMap() {
        Course course = new Course(List.of(student, otherStudent), seminarTeacher, lectureTeacher);
        AttendanceSheet attendanceSheet = new AttendanceSheet(course);
        LectureID lectureID = new LectureID(1);
        attendanceSheet.addLecture(lectureTeacher, lectureID, LocalDate.of(2026, 9, 14), "lab1");

        assertThat(attendanceSheet.getAttendances(lectureID)).isEqualTo(Collections.emptyMap());
    }
}
