package com.dev.csas.course;

import com.dev.csas.api.course.StudentCourseDetailsResponse;
import com.dev.csas.api.course.StudentCourseSummaryResponse;
import com.dev.csas.api.course.StudentControlResultResponse;
import com.dev.csas.control.ControlResult;
import com.dev.csas.control.ControlSheet;
import com.dev.csas.control.ControlSheetMapper;
import com.dev.csas.control.ControlSheetStatus;
import com.dev.csas.control.entity.ControlSheetJPAEntity;
import com.dev.csas.control.entity.ControlSheetRepository;
import com.dev.csas.discipline.ControlFormEnum;
import com.dev.csas.coursework.CourseworkGrade;
import com.dev.csas.coursework.CourseworkSheetRepository;
import com.dev.csas.coursework.CourseworkStudentResultReadModel;
import com.dev.csas.shared.StudentID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StudentCourseQueryService {

    private final CourseRepository courses;
    private final CourseMapper mapper;
    private final CourseworkSheetRepository courseworkSheets;
    private final ControlSheetRepository controlSheets;
    private final ControlSheetMapper controlSheetMapper;

    public StudentCourseQueryService(
            CourseRepository courses,
            CourseMapper mapper,
            CourseworkSheetRepository courseworkSheets,
            ControlSheetRepository controlSheets,
            ControlSheetMapper controlSheetMapper
    ) {
        this.courses = courses;
        this.mapper = mapper;
        this.courseworkSheets = courseworkSheets;
        this.controlSheets = controlSheets;
        this.controlSheetMapper = controlSheetMapper;
    }

    @Transactional(readOnly = true)
    public List<StudentCourseSummaryResponse> list(StudentID studentId) {
        List<CourseJPAEntity> courseEntities = courses.findAllByStudents_IdOrderByIdAsc(studentId.id());
        if (courseEntities.isEmpty()) {
            return List.of();
        }
        List<Long> courseIds = courseEntities.stream().map(CourseJPAEntity::getId).toList();
        Map<Long, CourseworkStudentResultReadModel> courseworkResults = courseworkSheets
                .findStudentResults(courseIds, studentId.id()).stream()
                .collect(Collectors.toMap(CourseworkStudentResultReadModel::courseId, Function.identity()));
        return courseEntities.stream()
                .map(entity -> {
                    Course course = mapper.toDomain(entity);
                    CourseworkStudentResultReadModel courseworkResult = courseworkResults.get(entity.getId());
                    Integer courseworkScore = courseworkResult == null ? null : courseworkResult.score();
                    return new StudentCourseSummaryResponse(
                            entity.getId(),
                            entity.getDiscipline().getName(),
                            entity.getGroup().getName(),
                            entity.getStartSemester().getName(),
                            entity.getEndSemester().getName(),
                            entity.getCourseType(),
                            course.status(),
                            course.type() == CourseType.SEMINAR ? course.rating(studentId) : null,
                            courseworkScore,
                            courseworkScore == null ? null : CourseworkGrade.fromScore(courseworkScore),
                            controlResult(entity.getId(), studentId)
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentCourseDetailsResponse get(StudentID studentId, long courseId) {
        CourseJPAEntity entity = courses.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("Курс не найден: " + courseId));
        Course course = mapper.toDomain(entity);
        if (!course.hasStudent(studentId)) {
            throw new CourseNotFoundException("Курс не найден: " + courseId);
        }

        List<StudentCourseDetailsResponse.WorkResult> works = course.works().stream()
                .sorted(Comparator.comparingLong(Work::getNumber))
                .map(work -> new StudentCourseDetailsResponse.WorkResult(
                        work.getNumber(),
                        work.getTitle(),
                        work.getMaxScore(),
                        work.getDate(),
                        course.scores().stream()
                                .filter(score -> score.getWorkNumber() == work.getNumber())
                                .filter(score -> score.getStudentId().equals(studentId))
                                .map(Score::getScore)
                                .findFirst()
                                .orElse(null)
                ))
                .toList();
        Integer courseworkScore = course.type() == CourseType.COURSEWORK
                ? courseworkSheets.findStudentResult(courseId, studentId.id())
                .map(com.dev.csas.coursework.CourseworkResultJPAEntity::getScore)
                .orElse(null)
                : null;

        return new StudentCourseDetailsResponse(
                entity.getId(),
                entity.getDiscipline().getName(),
                entity.getGroup().getName(),
                entity.getStartSemester().getName(),
                entity.getEndSemester().getName(),
                course.type(),
                course.status(),
                course.type() == CourseType.SEMINAR ? course.rating(studentId) : null,
                courseworkScore,
                courseworkScore == null ? null : CourseworkGrade.fromScore(courseworkScore),
                controlResult(entity.getId(), studentId),
                works
        );
    }

    private StudentControlResultResponse controlResult(long courseId, StudentID studentId) {
        return controlSheets.findByCourse_Id(courseId)
                .map(entity -> toStudentControlResult(entity, studentId))
                .orElse(null);
    }

    private StudentControlResultResponse toStudentControlResult(
            ControlSheetJPAEntity entity,
            StudentID studentId
    ) {
        ControlSheet sheet = controlSheetMapper.toDomain(entity);
        if (sheet.status() == ControlSheetStatus.OPEN) {
            return new StudentControlResultResponse(
                    sheet.controlForm(), sheet.status(), null, null, null, null
            );
        }

        ControlResult result = sheet.resultOf(studentId);
        if (sheet.controlForm() == ControlFormEnum.CREDIT) {
            return new StudentControlResultResponse(
                    sheet.controlForm(), sheet.status(), null, result.passFail(), null, null
            );
        }

        return new StudentControlResultResponse(
                sheet.controlForm(),
                sheet.status(),
                result.numericScore(),
                null,
                sheet.finalScore(studentId),
                sheet.finalGrade(studentId)
        );
    }
}
