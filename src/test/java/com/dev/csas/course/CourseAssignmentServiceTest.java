package com.dev.csas.course;

import com.dev.csas.api.course.CourseCreationRequest;
import com.dev.csas.attendance.AttendanceSheetRepository;
import com.dev.csas.attendance.AttendanceSheetJPAEntity;
import com.dev.csas.attendance.AttendanceSheetStatus;
import com.dev.csas.attendance.LectureJPAEntity;
import com.dev.csas.control.entity.ControlSheetRepository;
import com.dev.csas.coursework.CourseworkSheetRepository;
import com.dev.csas.coursework.CourseworkSheetJPAEntity;
import com.dev.csas.coursework.CourseworkSheetStatus;
import com.dev.csas.discipline.DisciplineJPAEntity;
import com.dev.csas.discipline.DisciplineRepository;
import com.dev.csas.error.CatalogConflictException;
import com.dev.csas.group.GroupJPAEntity;
import com.dev.csas.group.GroupRepository;
import com.dev.csas.semester.SemesterJPAEntity;
import com.dev.csas.semester.SemesterRepository;
import com.dev.csas.teacher.TeacherJPAEntity;
import com.dev.csas.teacher.TeacherRepository;
import com.dev.csas.student.StudentRepository;
import com.dev.csas.audit.UserActionService;
import com.dev.csas.audit.UserActionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseAssignmentServiceTest {

    @Mock
    private CourseRepository courses;
    @Mock
    private DisciplineRepository disciplines;
    @Mock
    private GroupRepository groups;
    @Mock
    private SemesterRepository semesters;
    @Mock
    private TeacherRepository teachers;
    @Mock
    private StudentRepository students;
    @Mock
    private CourseworkSheetRepository courseworkSheets;
    @Mock
    private AttendanceSheetRepository attendanceSheets;
    @Mock
    private ControlSheetRepository controlSheets;
    @Mock
    private UserActionService userActions;
    @InjectMocks
    private CourseAssignmentService service;

    private CourseJPAEntity course;
    private DisciplineJPAEntity discipline;
    private GroupJPAEntity group;
    private SemesterJPAEntity startSemester;
    private SemesterJPAEntity endSemester;
    private TeacherJPAEntity teacher;

    @BeforeEach
    void setUp() {
        discipline = DisciplineJPAEntity.builder().id(3L).name("Архитектура ЭВМ").build();
        group = GroupJPAEntity.builder().id(4L).name("ИВТ-22").build();
        startSemester = SemesterJPAEntity.builder()
                .id(5L).name("Семестр 3")
                .startDate(LocalDate.of(2026, 9, 1)).endDate(LocalDate.of(2027, 1, 31))
                .build();
        endSemester = SemesterJPAEntity.builder()
                .id(6L).name("Семестр 4")
                .startDate(LocalDate.of(2027, 2, 1)).endDate(LocalDate.of(2027, 6, 30))
                .build();
        teacher = TeacherJPAEntity.builder().id(7L).build();
        course = CourseJPAEntity.builder()
                .id(8L)
                .discipline(discipline)
                .group(group)
                .startSemester(startSemester)
                .endSemester(endSemester)
                .teacher(TeacherJPAEntity.builder().id(1L).build())
                .courseType(CourseType.SEMINAR)
                .works(new ArrayList<>())
                .build();

    }

    @Test
    void updatesCourseAssignmentFields() {
        stubAssignmentReferences(group);
        when(courses.findById(8L)).thenReturn(Optional.of(course));
        when(courses.existsByDiscipline_IdAndGroup_IdAndStartSemester_IdAndEndSemester_IdAndCourseTypeAndIdNot(
                3L, 4L, 5L, 6L, CourseType.SEMINAR, 8L
        )).thenReturn(false);
        when(courses.saveAndFlush(course)).thenReturn(course);

        var result = service.update(8L, request(4L, CourseType.SEMINAR));

        assertThat(result.id()).isEqualTo(8L);
        assertThat(course.getTeacher()).isSameAs(teacher);
        verify(courses).saveAndFlush(course);
        verify(userActions).record(UserActionType.COURSE_UPDATED, "COURSE", 8L,
                "Изменены поля: преподаватель");
    }

    @Test
    void refusesChangingGroupAfterAnAttendanceSheetExists() {
        GroupJPAEntity anotherGroup = GroupJPAEntity.builder().id(9L).name("ИБ-22").build();
        stubAssignmentReferences(anotherGroup);
        when(courses.findById(8L)).thenReturn(Optional.of(course));
        when(courses.existsByDiscipline_IdAndGroup_IdAndStartSemester_IdAndEndSemester_IdAndCourseTypeAndIdNot(
                3L, 9L, 5L, 6L, CourseType.SEMINAR, 8L
        )).thenReturn(false);
        when(controlSheets.existsByCourse_Id(8L)).thenReturn(false);
        AttendanceSheetJPAEntity sheet = AttendanceSheetJPAEntity.builder()
                .id(12L)
                .status(AttendanceSheetStatus.OPEN)
                .lectures(new ArrayList<>(java.util.List.of(LectureJPAEntity.builder().id(13L).build())))
                .build();
        when(attendanceSheets.findByCourse_Id(8L)).thenReturn(Optional.of(sheet));

        assertThatThrownBy(() -> service.update(8L, request(9L, CourseType.SEMINAR)))
                .isInstanceOf(CatalogConflictException.class)
                .hasMessageContaining("Нельзя изменить дисциплину, группу или период");
        verify(courses, never()).saveAndFlush(any());
    }

    @Test
    void deletesCourseWithoutWorkOrSheets() {
        when(courses.findById(8L)).thenReturn(Optional.of(course));
        when(controlSheets.existsByCourse_Id(8L)).thenReturn(false);

        service.delete(8L);

        verify(courses).delete(course);
        verify(userActions).record(UserActionType.COURSE_DELETED, "COURSE", 8L, null);
    }

    @Test
    void refusesDeletingCourseWithCourseworkSheet() {
        when(courses.findById(8L)).thenReturn(Optional.of(course));
        when(controlSheets.existsByCourse_Id(8L)).thenReturn(false);
        CourseworkSheetJPAEntity sheet = CourseworkSheetJPAEntity.builder()
                .id(14L)
                .status(CourseworkSheetStatus.ARCHIVED)
                .build();
        when(courseworkSheets.findByCourse_Id(8L)).thenReturn(Optional.of(sheet));

        assertThatThrownBy(() -> service.delete(8L))
                .isInstanceOf(CatalogConflictException.class)
                .hasMessageContaining("Нельзя удалить курс");
        verify(courses, never()).delete(any(CourseJPAEntity.class));
    }

    private CourseCreationRequest request(long groupId, CourseType type) {
        return new CourseCreationRequest(3L, groupId, 5L, 6L, type, 7L);
    }

    private void stubAssignmentReferences(GroupJPAEntity requestedGroup) {
        when(disciplines.findById(3L)).thenReturn(Optional.of(discipline));
        when(groups.findById(requestedGroup.getId())).thenReturn(Optional.of(requestedGroup));
        when(semesters.findById(5L)).thenReturn(Optional.of(startSemester));
        when(semesters.findById(6L)).thenReturn(Optional.of(endSemester));
        when(teachers.findById(7L)).thenReturn(Optional.of(teacher));
    }
}
