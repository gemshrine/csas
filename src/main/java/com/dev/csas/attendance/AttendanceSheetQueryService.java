package com.dev.csas.attendance;

import com.dev.csas.api.CourseHeaderResponse;
import com.dev.csas.api.attendance.AttendanceSheetDetailsResponse;
import com.dev.csas.api.attendance.AttendanceRosterStudentResponse;
import com.dev.csas.api.attendance.AttendanceStudentResponse;
import com.dev.csas.api.attendance.LectureDetailsResponse;
import com.dev.csas.course.CourseType;
import com.dev.csas.security.ActorAccessDeniedException;
import com.dev.csas.shared.TeacherID;
import com.dev.csas.student.StudentReadModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AttendanceSheetQueryService {

    private final AttendanceSheetRepository sheets;

    public AttendanceSheetQueryService(AttendanceSheetRepository sheets) {
        this.sheets = sheets;
    }

    @Transactional(readOnly = true)
    public AttendanceSheetDetailsResponse getForTeacher(long sheetId, TeacherID teacher) {
        AttendanceSheetJPAEntity sheet = findSheet(sheetId);
        if (sheet.getCourse().getCourseType() != CourseType.LECTURE
                || !sheet.getCourse().getTeacher().getId().equals(teacher.id())) {
            throw new ActorAccessDeniedException("Нет доступа к ведомости посещаемости этого курса");
        }
        return toResponse(sheet);
    }

    @Transactional(readOnly = true)
    public AttendanceSheetDetailsResponse getForDean(long sheetId) {
        return toResponse(findSheet(sheetId));
    }

    private AttendanceSheetJPAEntity findSheet(long sheetId) {
        return sheets.findDetailedById(sheetId)
                .orElseThrow(() -> new AttendanceSheetNotFoundException(
                        "Ведомость посещаемости не найдена: " + sheetId
                ));
    }

    private AttendanceSheetDetailsResponse toResponse(AttendanceSheetJPAEntity sheet) {
        List<StudentReadModel> students = sheets.findStudentReadModelsBySheetId(sheet.getId());
        Map<Long, Map<Long, AttendanceStatusEnum>> attendanceByLecture = sheets
                .findAttendanceMarkReadModelsBySheetId(sheet.getId()).stream()
                .collect(Collectors.groupingBy(
                        AttendanceMarkReadModel::lectureId,
                        Collectors.toMap(
                                AttendanceMarkReadModel::studentId,
                                mark -> mark.present() ? AttendanceStatusEnum.PRESENT : AttendanceStatusEnum.ABSENT
                        )
                ));

        List<LectureDetailsResponse> lectures = sheets.findLectureReadModelsBySheetId(sheet.getId()).stream()
                .map(lecture -> {
                    Map<Long, AttendanceStatusEnum> marks = attendanceByLecture.getOrDefault(lecture.id(), Map.of());
                    List<AttendanceStudentResponse> attendance = students.stream()
                            .map(student -> new AttendanceStudentResponse(
                                    student.id(),
                                    student.displayName(),
                                    marks.get(student.id())
                            ))
                            .toList();
                    return new LectureDetailsResponse(lecture.id(), lecture.date(), lecture.title(), attendance);
                })
                .toList();

        var course = sheet.getCourse();
        return new AttendanceSheetDetailsResponse(
                sheet.getId(),
                new CourseHeaderResponse(
                        course.getId(),
                        course.getDiscipline().getName(),
                        course.getGroup().getName(),
                        course.getStartSemester().getName(),
                        course.getEndSemester().getName(),
                        course.getCourseType(),
                        course.getStatus()
                ),
                sheet.getStatus(),
                students.stream()
                        .map(student -> new AttendanceRosterStudentResponse(student.id(), student.displayName()))
                        .toList(),
                lectures
        );
    }
}
