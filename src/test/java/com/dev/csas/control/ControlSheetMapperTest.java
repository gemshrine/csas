package com.dev.csas.control;

import com.dev.csas.control.entity.ControlResultJPAEntity;
import com.dev.csas.control.entity.ControlSheetJPAEntity;
import com.dev.csas.course.*;
import com.dev.csas.discipline.ControlFormEnum;
import com.dev.csas.group.GroupJPAEntity;
import com.dev.csas.shared.StudentID;
import com.dev.csas.shared.TeacherID;
import com.dev.csas.student.StudentJPAEntity;
import com.dev.csas.teacher.TeacherJPAEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ControlSheetMapperTest {

    private final ControlSheetMapper mapper = new ControlSheetMapper(new CourseMapper());

    @Test
    void toDomainRestoresSheetAndCourseForRatingCalculation() {
        StudentJPAEntity student = StudentJPAEntity.builder().id(7L).build();
        CourseWorkJPAEntity work = CourseWorkJPAEntity.builder()
                .id(41L)
                .number(1L)
                .title("Лабораторная")
                .maxScore(100)
                .date(LocalDate.of(2026, 9, 1))
                .scores(new ArrayList<>())
                .build();
        work.getScores().add(CourseScoreJPAEntity.builder()
                .id(51L)
                .work(work)
                .student(student)
                .score(60)
                .build());
        CourseJPAEntity course = courseEntity(student, work);
        work.setCourse(course);

        ControlResultJPAEntity result = ControlResultJPAEntity.builder()
                .id(61L)
                .student(student)
                .numericScore(80)
                .build();
        ControlSheetJPAEntity sheetEntity = ControlSheetJPAEntity.builder()
                .id(71L)
                .course(course)
                .controlForm(ControlFormEnum.EXAM)
                .status(ControlSheetStatus.CLOSED)
                .grade5From(90)
                .grade4From(80)
                .grade3From(70)
                .results(new ArrayList<>(List.of(result)))
                .build();
        result.setControlSheet(sheetEntity);

        ControlSheet sheet = mapper.toDomain(sheetEntity);

        assertEquals(ControlSheetStatus.CLOSED, sheet.status());
        assertEquals(ControlResult.numeric(80), sheet.resultOf(new StudentID(7L)));
        assertEquals(70.0, sheet.finalScore(new StudentID(7L)));
        assertEquals(3, sheet.finalGrade(new StudentID(7L)));
    }

    @Test
    void updateEntityChangesExistingResultAndDoesNotReplaceItsRow() {
        StudentJPAEntity student = StudentJPAEntity.builder().id(7L).build();
        CourseWorkJPAEntity work = CourseWorkJPAEntity.builder()
                .id(41L)
                .number(1L)
                .title("Лабораторная")
                .maxScore(100)
                .date(LocalDate.of(2026, 9, 1))
                .scores(new ArrayList<>())
                .build();
        CourseJPAEntity course = courseEntity(student, work);
        work.setCourse(course);

        ControlResultJPAEntity result = ControlResultJPAEntity.builder()
                .id(61L)
                .student(student)
                .numericScore(80)
                .build();
        ControlSheetJPAEntity target = ControlSheetJPAEntity.builder()
                .id(71L)
                .course(course)
                .controlForm(ControlFormEnum.EXAM)
                .status(ControlSheetStatus.OPEN)
                .grade5From(90)
                .grade4From(80)
                .grade3From(70)
                .results(new ArrayList<>(List.of(result)))
                .build();
        result.setControlSheet(target);

        ControlSheet sheet = mapper.toDomain(target);
        sheet.enterNumericResult(new TeacherID(11L), new StudentID(7L), 95, "Пересдача");
        mapper.updateEntity(sheet, target);

        assertSame(result, target.getResults().getFirst());
        assertEquals(95, result.getNumericScore());
        assertEquals(ControlSheetStatus.OPEN, target.getStatus());
        assertEquals(1, sheet.events().size());
    }

    private CourseJPAEntity courseEntity(StudentJPAEntity student, CourseWorkJPAEntity work) {
        return CourseJPAEntity.builder()
                .id(5L)
                .group(GroupJPAEntity.builder().id(3L).build())
                .courseType(CourseType.SEMINAR)
                .teacher(TeacherJPAEntity.builder().id(11L).build())
                .status(CourseStatus.COMPLETED)
                .students(new ArrayList<>(List.of(student)))
                .works(new ArrayList<>(List.of(work)))
                .build();
    }
}
