package com.dev.csas.course;

import com.dev.csas.api.course.CourseSummaryResponse;
import com.dev.csas.attendance.AttendanceSheetReadModel;
import com.dev.csas.attendance.AttendanceSheetRepository;
import com.dev.csas.control.ControlSheetReadModel;
import com.dev.csas.control.entity.ControlSheetRepository;
import com.dev.csas.coursework.CourseworkSheetReadModel;
import com.dev.csas.coursework.CourseworkSheetRepository;
import com.dev.csas.shared.TeacherID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CourseQueryService {

    private final CourseRepository courses;
    private final ControlSheetRepository controls;
    private final AttendanceSheetRepository attendance;
    private final CourseworkSheetRepository coursework;

    public CourseQueryService(
            CourseRepository courses,
            ControlSheetRepository controls,
            AttendanceSheetRepository attendance,
            CourseworkSheetRepository coursework
    ) {
        this.courses = courses;
        this.controls = controls;
        this.attendance = attendance;
        this.coursework = coursework;
    }

    @Transactional(readOnly = true)
    public List<CourseSummaryResponse> listForTeacher(TeacherID teacher) {
        List<CourseJPAEntity> courseEntities = courses.findAllByTeacher_IdOrderByIdAsc(teacher.id());
        return map(courseEntities, teacher.id());
    }

    @Transactional(readOnly = true)
    public List<CourseSummaryResponse> listForDean() {
        return map(courses.findAllByOrderByIdAsc(), null);
    }

    private List<CourseSummaryResponse> map(List<CourseJPAEntity> courseEntities, Long teacherId) {
        if (courseEntities.isEmpty()) {
            return List.of();
        }
        List<Long> courseIds = courseEntities.stream().map(CourseJPAEntity::getId).toList();
        Map<Long, ControlSheetReadModel> controlByCourse = controls.findCourseSheetReadModels(courseIds).stream()
                .collect(Collectors.toMap(ControlSheetReadModel::courseId, Function.identity()));
        Map<Long, AttendanceSheetReadModel> attendanceByCourse = attendance.findCourseSheetReadModels(courseIds).stream()
                .collect(Collectors.toMap(AttendanceSheetReadModel::courseId, Function.identity()));
        Map<Long, CourseworkSheetReadModel> courseworkByCourse = coursework
                .findCourseSheetReadModels(courseIds).stream()
                .collect(Collectors.toMap(CourseworkSheetReadModel::courseId, Function.identity()));

        return courseEntities.stream().map(course -> {
            ControlSheetReadModel control = controlByCourse.get(course.getId());
            AttendanceSheetReadModel attendanceSheet = attendanceByCourse.get(course.getId());
            CourseworkSheetReadModel courseworkSheet = courseworkByCourse.get(course.getId());
            boolean isSeminar = course.getCourseType() == CourseType.SEMINAR;
            boolean isLecture = course.getCourseType() == CourseType.LECTURE;
            boolean isCoursework = course.getCourseType() == CourseType.COURSEWORK;
            boolean canReadControl = isSeminar && (teacherId == null || course.getTeacher().getId().equals(teacherId));
            boolean canReadAttendance = isLecture && (teacherId == null || course.getTeacher().getId().equals(teacherId));
            return new CourseSummaryResponse(
                    course.getId(),
                    course.getDiscipline().getName(),
                    course.getGroup().getName(),
                    course.getStartSemester().getName(),
                    course.getEndSemester().getName(),
                    course.getCourseType(),
                    course.getStatus(),
                    canReadControl && control != null ? control.sheetId() : null,
                    canReadControl && control != null ? control.status() : null,
                    canReadAttendance && attendanceSheet != null ? attendanceSheet.sheetId() : null,
                    canReadAttendance && attendanceSheet != null ? attendanceSheet.status() : null,
                    isCoursework && courseworkSheet != null ? courseworkSheet.sheetId() : null,
                    isCoursework && courseworkSheet != null ? courseworkSheet.status() : null,
                    canReadControl,
                    canReadAttendance,
                    isCoursework && (teacherId == null || course.getTeacher().getId().equals(teacherId))
            );
        }).toList();
    }
}
