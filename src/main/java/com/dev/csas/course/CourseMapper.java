package com.dev.csas.course;

import com.dev.csas.shared.StudentID;
import com.dev.csas.shared.TeacherID;
import com.dev.csas.student.StudentJPAEntity;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {

    public Course toDomain(CourseJPAEntity entity) {
        Objects.requireNonNull(entity, "Курс не может быть пустым");

        List<StudentID> students = entity.getStudents().stream()
                .map(student -> new StudentID(student.getId()))
                .toList();

        List<Work> works = entity.getWorks().stream()
                .map(work -> new Work(
                        work.getNumber(),
                        work.getTitle(),
                        work.getMaxScore(),
                        work.getDate()
                ))
                .toList();

        List<Score> scores = entity.getWorks().stream()
                .flatMap(work -> work.getScores().stream())
                .map(score -> new Score(
                        score.getWork().getNumber(),
                        new StudentID(score.getStudent().getId()),
                        score.getScore()
                ))
                .toList();

        return Course.reconstitute(
                students,
                entity.getCourseType(),
                new TeacherID(entity.getTeacher().getId()),
                entity.getStatus(),
                works,
                scores
        );
    }

    public void updateOwnedData(
            Course source,
            CourseJPAEntity target,
            Function<StudentID, StudentJPAEntity> studentResolver
    ) {
        Objects.requireNonNull(source, "Курс не может быть пустым");
        Objects.requireNonNull(target, "JPA-курс не может быть пустым");
        Objects.requireNonNull(studentResolver, "Преобразователь студентов обязателен");

        target.setStatus(source.status());

        Map<StudentID, StudentJPAEntity> resolvedStudents = source.students().stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        studentID -> Objects.requireNonNull(
                                studentResolver.apply(studentID),
                                "Студент не найден: " + studentID.id()
                        )
                ));
        target.getStudents().clear();
        target.getStudents().addAll(resolvedStudents.values());

        Map<Long, CourseWorkJPAEntity> existingWorks = target.getWorks().stream()
                .collect(Collectors.toMap(CourseWorkJPAEntity::getNumber, Function.identity()));
        Set<Long> retainedWorkNumbers = new HashSet<>();

        for (Work work : source.works()) {
            retainedWorkNumbers.add(work.getNumber());
            CourseWorkJPAEntity workEntity = existingWorks.get(work.getNumber());
            if (workEntity == null) {
                workEntity = CourseWorkJPAEntity.builder()
                        .course(target)
                        .number(work.getNumber())
                        .build();
                target.getWorks().add(workEntity);
            }

            workEntity.setTitle(work.getTitle());
            workEntity.setMaxScore(work.getMaxScore());
            workEntity.setDate(work.getDate());
            updateScores(source, work, workEntity, resolvedStudents);
        }

        target.getWorks().removeIf(work -> !retainedWorkNumbers.contains(work.getNumber()));
    }

    private void updateScores(
            Course source,
            Work work,
            CourseWorkJPAEntity workEntity,
            Map<StudentID, StudentJPAEntity> students
    ) {
        Map<StudentID, CourseScoreJPAEntity> existingScores = workEntity.getScores().stream()
                .collect(Collectors.toMap(
                        score -> new StudentID(score.getStudent().getId()),
                        Function.identity()
                ));
        Set<StudentID> retainedStudents = new HashSet<>();

        for (Score score : source.scores()) {
            if (score.getWorkNumber() != work.getNumber()) {
                continue;
            }

            StudentID studentID = score.getStudentId();
            retainedStudents.add(studentID);
            CourseScoreJPAEntity scoreEntity = existingScores.get(studentID);
            if (scoreEntity == null) {
                scoreEntity = CourseScoreJPAEntity.builder()
                        .work(workEntity)
                        .student(Optional.ofNullable(students.get(studentID))
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "Оценка ссылается на студента вне курса: " + studentID.id()
                                )))
                        .build();
                workEntity.getScores().add(scoreEntity);
            }
            scoreEntity.setScore(score.getScore());
        }

        workEntity.getScores().removeIf(score ->
                !retainedStudents.contains(new StudentID(score.getStudent().getId())));
    }

}
