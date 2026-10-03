package com.dev.csas.coursework;

import com.dev.csas.api.CourseHeaderResponse;
import com.dev.csas.course.CourseMapper;
import com.dev.csas.security.ActorAccessDeniedException;
import com.dev.csas.shared.TeacherID;
import com.dev.csas.student.StudentJPAEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseworkSheetQueryService {

    private final CourseworkSheetRepository sheets;
    private final CourseworkSheetMapper mapper;

    public CourseworkSheetQueryService(CourseworkSheetRepository sheets, CourseworkSheetMapper mapper) {
        this.sheets = sheets;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public CourseworkSheetDetailsResponse getForTeacher(long sheetId, TeacherID teacher) {
        CourseworkSheetJPAEntity sheet = findSheet(sheetId);
        if (!sheet.getCourse().getTeacher().getId().equals(teacher.id())) {
            throw new ActorAccessDeniedException("Нет доступа к ведомости курсовой работы этого курса");
        }
        return toResponse(sheet);
    }

    @Transactional(readOnly = true)
    public CourseworkSheetDetailsResponse getForDean(long sheetId) {
        return toResponse(findSheet(sheetId));
    }

    private CourseworkSheetJPAEntity findSheet(long sheetId) {
        return sheets.findDetailedById(sheetId)
                .orElseThrow(() -> new CourseworkSheetNotFoundException(
                        "Ведомость курсовой работы не найдена: " + sheetId
                ));
    }

    private CourseworkSheetDetailsResponse toResponse(CourseworkSheetJPAEntity entity) {
        CourseworkSheet sheet = mapper.toDomain(entity);
        Map<Long, Integer> scores = sheet.scores().entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().id(), Map.Entry::getValue));
        var course = entity.getCourse();
        return new CourseworkSheetDetailsResponse(
                entity.getId(),
                new CourseHeaderResponse(
                        course.getId(), course.getDiscipline().getName(), course.getGroup().getName(),
                        course.getStartSemester().getName(), course.getEndSemester().getName(),
                        course.getCourseType(), course.getStatus()
                ),
                entity.getStatus(),
                course.getStudents().stream()
                        .sorted(java.util.Comparator.comparing(StudentJPAEntity::getLastName)
                                .thenComparing(StudentJPAEntity::getFirstName)
                                .thenComparing(StudentJPAEntity::getId))
                        .map(student -> {
                            Integer score = scores.get(student.getId());
                            return new CourseworkSheetDetailsResponse.StudentResult(
                                    student.getId(), displayName(student), score,
                                    score == null ? null : sheet.gradeOfScore(score)
                            );
                        })
                        .toList()
        );
    }

    private String displayName(StudentJPAEntity student) {
        return student.getLastName() + " " + student.getFirstName()
                + (student.getPatronymic() == null ? "" : " " + student.getPatronymic());
    }
}
