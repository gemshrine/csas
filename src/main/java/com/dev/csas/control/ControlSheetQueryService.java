package com.dev.csas.control;

import com.dev.csas.api.CourseHeaderResponse;
import com.dev.csas.api.control.ControlSheetDetailsResponse;
import com.dev.csas.api.control.ControlStudentResultResponse;
import com.dev.csas.api.control.GradingScaleResponse;
import com.dev.csas.control.entity.ControlSheetJPAEntity;
import com.dev.csas.control.entity.ControlSheetRepository;
import com.dev.csas.security.ActorAccessDeniedException;
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

    public ControlSheetQueryService(ControlSheetRepository sheets) {
        this.sheets = sheets;
    }

    @Transactional(readOnly = true)
    public ControlSheetDetailsResponse getForTeacher(long sheetId, TeacherID teacher) {
        ControlSheetJPAEntity sheet = findSheet(sheetId);
        if (!sheet.getCourse().getSeminarTeacher().getId().equals(teacher.id())) {
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
        Map<Long, ControlResultReadModel> results = sheets.findResultReadModelsBySheetId(sheet.getId()).stream()
                .collect(Collectors.toMap(ControlResultReadModel::studentId, Function.identity()));
        var students = sheets.findStudentReadModelsBySheetId(sheet.getId()).stream()
                .map(student -> toStudentResponse(student, results.get(student.id())))
                .toList();
        var course = sheet.getCourse();

        return new ControlSheetDetailsResponse(
                sheet.getId(),
                new CourseHeaderResponse(
                        course.getId(),
                        course.getDiscipline().getName(),
                        course.getGroup().getName(),
                        course.getSemester().getName(),
                        course.getStatus()
                ),
                sheet.getControlForm(),
                sheet.getStatus(),
                new GradingScaleResponse(sheet.getGrade5From(), sheet.getGrade4From(), sheet.getGrade3From()),
                students
        );
    }

    private ControlStudentResultResponse toStudentResponse(StudentReadModel student, ControlResultReadModel result) {
        return new ControlStudentResultResponse(
                student.id(),
                student.displayName(),
                result == null ? null : result.numericScore(),
                result == null ? null : result.passFail()
        );
    }
}
