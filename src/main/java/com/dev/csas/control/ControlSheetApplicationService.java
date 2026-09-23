package com.dev.csas.control;

import com.dev.csas.control.audit.ControlSheetEvent;
import com.dev.csas.control.audit.ControlSheetEventJPAEntity;
import com.dev.csas.control.audit.ControlSheetEventMapper;
import com.dev.csas.control.audit.ControlSheetEventRepository;
import com.dev.csas.control.entity.ControlSheetJPAEntity;
import com.dev.csas.control.entity.ControlSheetRepository;
import com.dev.csas.shared.DeanID;
import com.dev.csas.shared.StudentID;
import com.dev.csas.shared.TeacherID;
import com.dev.csas.student.StudentJPAEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ControlSheetApplicationService {

    private final ControlSheetRepository sheets;
    private final ControlSheetEventRepository events;
    private final ControlSheetMapper mapper;
    private final ControlSheetEventMapper eventMapper;

    public ControlSheetApplicationService(
            ControlSheetRepository sheets,
            ControlSheetEventRepository events,
            ControlSheetMapper mapper,
            ControlSheetEventMapper eventMapper
    ) {
        this.sheets = sheets;
        this.events = events;
        this.mapper = mapper;
        this.eventMapper = eventMapper;
    }

    @Transactional
    public void enterNumericResult(
            long sheetId,
            TeacherID teacher,
            StudentID student,
            int score,
            String reason
    ) {
        mutate(sheetId, sheet -> sheet.enterNumericResult(teacher, student, score, reason));
    }

    @Transactional
    public void enterPassFailResult(
            long sheetId,
            TeacherID teacher,
            StudentID student,
            PassFail result,
            String reason
    ) {
        mutate(sheetId, sheet -> sheet.enterPassFailResult(teacher, student, result, reason));
    }

    @Transactional
    public void close(long sheetId, TeacherID teacher) {
        mutate(sheetId, sheet -> sheet.close(teacher));
    }

    @Transactional
    public void reopen(long sheetId, DeanID dean, String reason) {
        mutate(sheetId, sheet -> sheet.reopen(dean, reason));
    }

    @Transactional
    public void archive(long sheetId, DeanID dean, String reason) {
        mutate(sheetId, sheet -> sheet.archive(dean, reason));
    }

    private void mutate(long sheetId, Consumer<ControlSheet> operation) {
        ControlSheetJPAEntity entity = sheets.findById(sheetId)
                .orElseThrow(() -> new ControlSheetException("Контрольная ведомость не найдена: " + sheetId));

        ControlSheet sheet = mapper.toDomain(entity);
        operation.accept(sheet);
        mapper.updateEntity(sheet, entity);
        persistEvents(sheet, entity);
    }

    private void persistEvents(ControlSheet sheet, ControlSheetJPAEntity entity) {
        Map<Long, StudentJPAEntity> students = entity.getCourse().getStudents().stream()
                .collect(Collectors.toMap(StudentJPAEntity::getId, Function.identity()));

        for (ControlSheetEvent event : sheet.events()) {
            StudentJPAEntity student = event.studentID() == null
                    ? null
                    : students.get(event.studentID().id());
            if (event.studentID() != null && student == null) {
                throw new ControlSheetException(
                        "Студент события не найден в составе курса: " + event.studentID().id()
                );
            }

            ControlSheetEventJPAEntity eventEntity = eventMapper.toEntity(event, entity, student);
            events.save(eventEntity);
        }
    }
}
