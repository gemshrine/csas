package com.dev.csas.course;

import com.dev.csas.api.course.StudentCourseDetailsResponse;
import com.dev.csas.api.course.StudentCourseSummaryResponse;
import com.dev.csas.shared.StudentID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class StudentCourseQueryService {

    private final CourseRepository courses;
    private final CourseMapper mapper;

    public StudentCourseQueryService(CourseRepository courses, CourseMapper mapper) {
        this.courses = courses;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<StudentCourseSummaryResponse> list(StudentID studentId) {
        return courses.findAllByStudents_IdOrderByIdAsc(studentId.id()).stream()
                .map(entity -> {
                    Course course = mapper.toDomain(entity);
                    return new StudentCourseSummaryResponse(
                            entity.getId(),
                            entity.getDiscipline().getName(),
                            entity.getGroup().getName(),
                            entity.getSemester().getName(),
                            course.status(),
                            course.rating(studentId)
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

        return new StudentCourseDetailsResponse(
                entity.getId(),
                entity.getDiscipline().getName(),
                entity.getGroup().getName(),
                entity.getSemester().getName(),
                course.status(),
                course.rating(studentId),
                works
        );
    }
}
