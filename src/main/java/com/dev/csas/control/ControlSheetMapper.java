package com.dev.csas.control;

import com.dev.csas.control.entity.ControlResultJPAEntity;
import com.dev.csas.control.entity.ControlSheetJPAEntity;
import com.dev.csas.course.CourseMapper;
import com.dev.csas.shared.StudentID;
import com.dev.csas.student.StudentJPAEntity;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ControlSheetMapper {

    private final CourseMapper courseMapper;

    public ControlSheetMapper(CourseMapper courseMapper) {
        this.courseMapper = courseMapper;
    }

    public ControlSheet toDomain(ControlSheetJPAEntity entity) {
        Objects.requireNonNull(entity, "Ведомость не может быть пустой");

        Map<StudentID, ControlResult> results = entity.getResults().stream()
                .collect(Collectors.toMap(
                        result -> new StudentID(result.getStudent().getId()),
                        result -> new ControlResult(result.getNumericScore(), result.getPassFail())
                ));

        return ControlSheet.reconstitute(
                courseMapper.toDomain(entity.getCourse()),
                entity.getControlForm(),
                new GradingScale(entity.getGrade5From(), entity.getGrade4From(), entity.getGrade3From()),
                entity.getStatus(),
                results
        );
    }

    public void updateEntity(ControlSheet source, ControlSheetJPAEntity target) {
        Objects.requireNonNull(source, "Доменная ведомость не может быть пустой");
        Objects.requireNonNull(target, "JPA-ведомость не может быть пустой");

        target.setStatus(source.status());

        Map<StudentID, StudentJPAEntity> students = target.getCourse().getStudents().stream()
                .collect(Collectors.toMap(
                        student -> new StudentID(student.getId()),
                        Function.identity()
                ));
        Map<StudentID, ControlResultJPAEntity> currentResults = target.getResults().stream()
                .collect(Collectors.toMap(
                        result -> new StudentID(result.getStudent().getId()),
                        Function.identity()
                ));
        Set<StudentID> retainedStudents = new HashSet<>();

        source.results().forEach((studentID, result) -> {
            StudentJPAEntity student = Optional.ofNullable(students.get(studentID))
                    .orElseThrow(() -> new ControlSheetException(
                            "Студент ведомости не найден в составе курса: " + studentID.id()
                    ));
            retainedStudents.add(studentID);

            ControlResultJPAEntity resultEntity = currentResults.get(studentID);
            if (resultEntity == null) {
                resultEntity = ControlResultJPAEntity.builder()
                        .controlSheet(target)
                        .student(student)
                        .build();
                target.getResults().add(resultEntity);
            }

            resultEntity.setNumericScore(result.numericScore());
            resultEntity.setPassFail(result.passFail());
        });

        target.getResults().removeIf(result -> !retainedStudents.contains(new StudentID(result.getStudent().getId())));
    }
}
