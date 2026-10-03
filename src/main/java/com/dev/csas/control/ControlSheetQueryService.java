package com.dev.csas.control;

import com.dev.csas.api.CourseHeaderResponse;
import com.dev.csas.api.control.ControlSheetDetailsResponse;
import com.dev.csas.api.control.ControlStudentResultResponse;
import com.dev.csas.api.control.GradingScaleResponse;
import com.dev.csas.control.entity.ControlSheetJPAEntity;
import com.dev.csas.control.entity.ControlSheetRepository;
import com.dev.csas.course.CourseType;
import com.dev.csas.discipline.ControlFormEnum;
import com.dev.csas.security.ActorAccessDeniedException;
import com.dev.csas.shared.StudentID;
import com.dev.csas.shared.TeacherID;
import com.dev.csas.student.StudentReadModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ControlSheetQueryService {

    private final ControlSheetRepository sheets;
    private final ControlSheetMapper mapper;

    public ControlSheetQueryService(ControlSheetRepository sheets, ControlSheetMapper mapper) {
        this.sheets = sheets;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ControlSheetDetailsResponse getForTeacher(long sheetId, TeacherID teacher) {
        ControlSheetJPAEntity sheet = findSheet(sheetId);
        if (sheet.getCourse().getCourseType() != CourseType.SEMINAR
                || !sheet.getCourse().getTeacher().getId().equals(teacher.id())) {
            throw new ActorAccessDeniedException("Нет доступа к контрольной ведомости этого курса");
        }
        return toResponse(sheet);
    }

    @Transactional(readOnly = true)
    public ControlSheetDetailsResponse getForDean(long sheetId) {
        return toResponse(findSheet(sheetId));
    }

    private ControlSheetJPAEntity findSheet(long sheetId) {
        return sheets.findDetailedById(sheetId)
                .orElseThrow(() -> new ControlSheetNotFoundException("Контрольная ведомость не найдена: " + sheetId));
    }

    private ControlSheetDetailsResponse toResponse(ControlSheetJPAEntity sheet) {
        ControlSheet domainSheet = mapper.toDomain(sheet);
        Map<Long, ControlResultReadModel> results = sheets.findResultReadModelsBySheetId(sheet.getId()).stream()
                .collect(Collectors.toMap(ControlResultReadModel::studentId, Function.identity()));
        var students = sheets.findStudentReadModelsBySheetId(sheet.getId()).stream()
                .map(student -> toStudentResponse(student, results.get(student.id()), domainSheet))
                .toList();
        var course = sheet.getCourse();

        return new ControlSheetDetailsResponse(
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
                sheet.getControlForm(),
                sheet.getStatus(),
                new GradingScaleResponse(sheet.getGrade5From(), sheet.getGrade4From(), sheet.getGrade3From()),
                students
        );
    }

    private ControlStudentResultResponse toStudentResponse(
            StudentReadModel student,
            ControlResultReadModel result,
            ControlSheet domainSheet
    ) {
        StudentID studentId = new StudentID(student.id());
        boolean hasFinalNumericResult = result != null
                && domainSheet.status() != ControlSheetStatus.OPEN
                && domainSheet.controlForm() != ControlFormEnum.CREDIT;
        return new ControlStudentResultResponse(
                student.id(),
                student.displayName(),
                result == null ? null : result.numericScore(),
                result == null ? null : result.passFail(),
                hasFinalNumericResult ? domainSheet.finalScore(studentId) : null,
                hasFinalNumericResult ? domainSheet.finalGrade(studentId) : null
        );
    }
}
