package com.dev.csas.course;

import com.dev.csas.group.GroupJPAEntity;
import com.dev.csas.shared.StudentID;
import com.dev.csas.student.StudentJPAEntity;
import com.dev.csas.teacher.TeacherJPAEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CourseMapperTest {

    private final CourseMapper mapper = new CourseMapper();

    @Test
    void toDomainRestoresRosterWorksScoresTeachersAndStatus() {
        StudentJPAEntity student = StudentJPAEntity.builder().id(7L).build();
        CourseWorkJPAEntity work = CourseWorkJPAEntity.builder()
                .id(41L)
                .number(1L)
                .title("Лабораторная")
                .maxScore(80)
                .date(LocalDate.of(2026, 9, 1))
                .scores(new ArrayList<>())
                .build();
        CourseScoreJPAEntity score = CourseScoreJPAEntity.builder()
                .id(51L)
                .work(work)
                .student(student)
                .score(60)
                .build();
        work.getScores().add(score);

        CourseJPAEntity entity = CourseJPAEntity.builder()
                .id(5L)
                .group(GroupJPAEntity.builder().id(3L).build())
                .seminarTeacher(TeacherJPAEntity.builder().id(11L).build())
                .lectureTeacher(TeacherJPAEntity.builder().id(12L).build())
                .status(CourseStatus.COMPLETED)
                .students(new ArrayList<>(List.of(student)))
                .works(new ArrayList<>(List.of(work)))
                .build();
        work.setCourse(entity);

        Course course = mapper.toDomain(entity);

        assertTrue(course.isCompleted());
        assertTrue(course.hasStudent(new StudentID(7L)));
        assertEquals(11L, course.seminarTeacher().id());
        assertEquals(12L, course.lectureTeacher().id());
        assertEquals(75.0, course.rating(new StudentID(7L)));
    }

    @Test
    void updateOwnedDataUpdatesWorkAndStudentScoreWithoutReplacingRows() {
        StudentJPAEntity student = StudentJPAEntity.builder().id(7L).build();
        CourseWorkJPAEntity work = CourseWorkJPAEntity.builder()
                .id(41L)
                .number(1L)
                .title("Лабораторная")
                .maxScore(80)
                .date(LocalDate.of(2026, 9, 1))
                .scores(new ArrayList<>())
                .build();
        CourseScoreJPAEntity scoreEntity = CourseScoreJPAEntity.builder()
                .id(51L)
                .work(work)
                .student(student)
                .score(60)
                .build();
        work.getScores().add(scoreEntity);

        CourseJPAEntity target = CourseJPAEntity.builder()
                .id(5L)
                .status(CourseStatus.IN_PROGRESS)
                .students(new ArrayList<>(List.of(student)))
                .works(new ArrayList<>(List.of(work)))
                .build();
        work.setCourse(target);

        Course source = new Course(List.of(new StudentID(7L)));
        source.addWork("Лабораторная обновлённая", 100, LocalDate.of(2026, 9, 2));
        source.score(1, new StudentID(7L), 90);

        mapper.updateOwnedData(source, target, id -> student);

        assertEquals(CourseStatus.IN_PROGRESS, target.getStatus());
        assertSame(work, target.getWorks().getFirst());
        assertEquals("Лабораторная обновлённая", work.getTitle());
        assertEquals(100, work.getMaxScore());
        assertEquals(LocalDate.of(2026, 9, 2), work.getDate());
        assertSame(scoreEntity, work.getScores().getFirst());
        assertEquals(90, scoreEntity.getScore());
    }
}
