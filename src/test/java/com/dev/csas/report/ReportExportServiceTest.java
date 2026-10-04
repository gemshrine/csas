package com.dev.csas.report;

import com.dev.csas.api.control.ControlSheetDetailsResponse;
import com.dev.csas.api.control.ControlStudentResultResponse;
import com.dev.csas.api.control.GradingScaleResponse;
import com.dev.csas.api.course.CourseDetailsResponse;
import com.dev.csas.control.ControlSheetQueryService;
import com.dev.csas.control.ControlSheetStatus;
import com.dev.csas.control.entity.ControlSheetJPAEntity;
import com.dev.csas.control.entity.ControlSheetRepository;
import com.dev.csas.discipline.ControlFormEnum;
import com.dev.csas.discipline.DisciplineJPAEntity;
import com.dev.csas.course.CourseJPAEntity;
import com.dev.csas.course.CourseApplicationService;
import com.dev.csas.course.CourseRepository;
import com.dev.csas.course.CourseStatus;
import com.dev.csas.course.CourseType;
import com.dev.csas.course.StudentCourseQueryService;
import com.dev.csas.attendance.AttendanceSheetQueryService;
import com.dev.csas.attendance.AttendanceSheetRepository;
import com.dev.csas.coursework.CourseworkSheetQueryService;
import com.dev.csas.coursework.CourseworkSheetRepository;
import com.dev.csas.group.GroupRepository;
import com.dev.csas.group.GroupJPAEntity;
import com.dev.csas.semester.SemesterJPAEntity;
import com.dev.csas.shared.StudentID;
import com.dev.csas.student.StudentJPAEntity;
import com.dev.csas.student.StudentRepository;
import com.dev.csas.api.course.StudentCourseSummaryResponse;
import com.dev.csas.api.course.StudentControlResultResponse;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportExportServiceTest {

    @Mock
    private CourseApplicationService courses;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentCourseQueryService studentCourses;

    @Mock
    private ControlSheetRepository controlSheetRepository;

    @Mock
    private ControlSheetQueryService controlSheets;

    @Mock
    private AttendanceSheetRepository attendanceSheetRepository;

    @Mock
    private AttendanceSheetQueryService attendanceSheets;

    @Mock
    private CourseworkSheetRepository courseworkSheetRepository;

    @Mock
    private CourseworkSheetQueryService courseworkSheets;

    private ReportExportService reports;

    @BeforeEach
    void setUp() {
        reports = new ReportExportService(new XlsxReportWriter(), courses, courseRepository, groupRepository,
                studentRepository, studentCourses, controlSheetRepository, controlSheets,
                attendanceSheetRepository, attendanceSheets, courseworkSheetRepository, courseworkSheets);
    }

    @Test
    void exportsCourseRatingAndFinalControlValuesFromQueryResponses() throws Exception {
        var work = new CourseDetailsResponse.CourseWorkResponse(1, "Контрольная", 20,
                LocalDate.of(2026, 10, 2), List.of(
                new CourseDetailsResponse.CourseWorkScoreResponse(3, 18)));
        CourseDetailsResponse course = new CourseDetailsResponse(5, "Архитектура ЭВМ", "ИВТ-22",
                "3 семестр", "4 семестр", CourseType.SEMINAR, CourseStatus.COMPLETED,
                List.of(work), List.of(new CourseDetailsResponse.CourseStudentResponse(3, "Иванов Иван", 93.0)));
        ControlSheetJPAEntity sheet = mock(ControlSheetJPAEntity.class);
        when(sheet.getId()).thenReturn(17L);
        when(courses.getForDean(5)).thenReturn(course);
        when(controlSheetRepository.findByCourse_Id(5L)).thenReturn(Optional.of(sheet));
        when(controlSheets.getForDean(17)).thenReturn(new ControlSheetDetailsResponse(
                17, null, com.dev.csas.discipline.ControlFormEnum.EXAM, ControlSheetStatus.CLOSED,
                new GradingScaleResponse(85, 70, 50),
                List.of(new ControlStudentResultResponse(3, "Иванов Иван", 80, null, 86.5, 5))));

        byte[] bytes = reports.course(5, null, true);

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            var sheetData = workbook.getSheet("Итоговая ведомость");
            assertEquals("Рейтинг, %", sheetData.getRow(7).getCell(2).getStringCellValue());
            assertEquals(18.0, sheetData.getRow(8).getCell(1).getNumericCellValue());
            assertEquals(93.0, sheetData.getRow(8).getCell(2).getNumericCellValue());
            assertEquals("Экзамен", sheetData.getRow(8).getCell(3).getStringCellValue());
            assertEquals(80.0, sheetData.getRow(8).getCell(4).getNumericCellValue());
            assertEquals(86.5, sheetData.getRow(8).getCell(5).getNumericCellValue());
            assertEquals(5.0, sheetData.getRow(8).getCell(6).getNumericCellValue());
        }
    }

    @Test
    void statisticsUsesFinalGradesAndLeavesQualityBlankWhenThereAreNoGrades() throws Exception {
        stubStatisticsSource();
        StudentCourseSummaryResponse record = new StudentCourseSummaryResponse(
                5, "Дискретная математика", "ИВТ-22", "3 семестр", "4 семестр",
                CourseType.SEMINAR, CourseStatus.COMPLETED, 100.0, null, null,
                new StudentControlResultResponse(ControlFormEnum.EXAM, ControlSheetStatus.CLOSED,
                        90, null, 91.5, 4));
        when(studentCourses.list(new StudentID(3))).thenReturn(List.of(record));

        byte[] withGrade = reports.statistics();

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(withGrade))) {
            var groupSheet = workbook.getSheet("По группам");
            assertEquals(1.0, groupSheet.getRow(4).getCell(4).getNumericCellValue());
            assertEquals(1.0, groupSheet.getRow(4).getCell(5).getNumericCellValue());
            assertEquals(100.0, groupSheet.getRow(4).getCell(6).getNumericCellValue());
            assertEquals(100.0, workbook.getSheet("Качество знаний")
                    .getRow(6).getCell(3).getNumericCellValue());
        }

        when(studentCourses.list(new StudentID(3))).thenReturn(List.of(new StudentCourseSummaryResponse(
                5, "Дискретная математика", "ИВТ-22", "3 семестр", "4 семестр",
                CourseType.SEMINAR, CourseStatus.COMPLETED, 100.0, null, null, null)));

        byte[] withoutGrade = reports.statistics();

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(withoutGrade))) {
            assertEquals(org.apache.poi.ss.usermodel.CellType.BLANK,
                    workbook.getSheet("По группам").getRow(4).getCell(6).getCellType());
        }
    }

    private void stubStatisticsSource() {
        GroupJPAEntity group = mock(GroupJPAEntity.class);
        when(group.getId()).thenReturn(8L);
        when(group.getName()).thenReturn("ИВТ-22");
        when(group.getSpecialty()).thenReturn("Информатика");

        StudentJPAEntity student = mock(StudentJPAEntity.class);
        when(student.getId()).thenReturn(3L);
        when(student.getGroup()).thenReturn(group);

        CourseJPAEntity course = mock(CourseJPAEntity.class);
        when(course.getId()).thenReturn(5L);
        when(course.getGroup()).thenReturn(group);
        when(course.getDiscipline()).thenReturn(mock(DisciplineJPAEntity.class));
        when(course.getStartSemester()).thenReturn(mock(SemesterJPAEntity.class));
        when(course.getEndSemester()).thenReturn(mock(SemesterJPAEntity.class));
        when(course.getCourseType()).thenReturn(CourseType.SEMINAR);
        when(course.getStatus()).thenReturn(CourseStatus.COMPLETED);
        when(course.getStudents()).thenReturn(List.of(student));
        when(course.getDiscipline().getName()).thenReturn("Дискретная математика");
        when(course.getStartSemester().getName()).thenReturn("3 семестр");
        when(course.getEndSemester().getName()).thenReturn("4 семестр");

        when(courseRepository.findAllByOrderByIdAsc()).thenReturn(List.of(course));
        when(courseworkSheetRepository.findCourseSheetReadModels(List.of(5L))).thenReturn(List.of());
        when(groupRepository.findAll()).thenReturn(List.of(group));
        when(studentRepository.findAll()).thenReturn(List.of(student));
        when(studentRepository.findAllByGroup_IdOrderByIdAsc(8L)).thenReturn(List.of(student));
    }
}
