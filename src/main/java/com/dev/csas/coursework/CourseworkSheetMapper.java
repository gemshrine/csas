package com.dev.csas.coursework;

import com.dev.csas.course.Course;
import com.dev.csas.course.CourseMapper;
import com.dev.csas.shared.StudentID;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class CourseworkSheetMapper {

    private final CourseMapper courseMapper;

    public CourseworkSheetMapper(CourseMapper courseMapper) {
        this.courseMapper = courseMapper;
    }

    public CourseworkSheet toDomain(CourseworkSheetJPAEntity entity) {
        Objects.requireNonNull(entity, "Ведомость курсовых работ не может быть пустой");
        Course course = courseMapper.toDomain(entity.getCourse());
        Map<StudentID, Integer> scores = entity.getResults().stream()
                .collect(Collectors.toMap(
                        result -> new StudentID(result.getStudent().getId()),
                        CourseworkResultJPAEntity::getScore
                ));
        return CourseworkSheet.reconstitute(course, entity.getStatus(), scores);
    }

    public void updateEntity(CourseworkSheet source, CourseworkSheetJPAEntity target) {
        Objects.requireNonNull(source, "Доменная ведомость курсовых работ не может быть пустой");
        Objects.requireNonNull(target, "JPA-ведомость курсовых работ не может быть пустой");
        target.setStatus(source.status());

        Map<StudentID, CourseworkResultJPAEntity> current = target.getResults().stream()
                .collect(Collectors.toMap(
                        result -> new StudentID(result.getStudent().getId()),
                        result -> result
                ));
        Map<Long, com.dev.csas.student.StudentJPAEntity> students = target.getCourse().getStudents().stream()
                .collect(Collectors.toMap(
                        com.dev.csas.student.StudentJPAEntity::getId,
                        student -> student
                ));

        source.scores().forEach((studentId, score) -> {
            var student = students.get(studentId.id());
            if (student == null) {
                throw new CourseworkSheetStudentNotFoundException(
                        "Студент не найден в составе курсового курса: " + studentId.id()
                );
            }
            CourseworkResultJPAEntity result = current.get(studentId);
            if (result == null) {
                result = CourseworkResultJPAEntity.builder()
                        .courseworkSheet(target)
                        .student(student)
                        .build();
                target.getResults().add(result);
            }
            result.setScore(score);
        });

        target.getResults().removeIf(result ->
                !source.scores().containsKey(new StudentID(result.getStudent().getId())));
    }
}
