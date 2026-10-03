package com.dev.csas.coursework;

import com.dev.csas.shared.DeanID;
import com.dev.csas.shared.StudentID;
import com.dev.csas.shared.TeacherID;
import com.dev.csas.error.CatalogBadRequestException;
import com.dev.csas.error.CatalogNotFoundException;
import com.dev.csas.semester.SemesterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;

@Service
public class CourseworkSheetApplicationService {

    private final CourseworkSheetRepository sheets;
    private final CourseworkSheetMapper mapper;
    private final SemesterRepository semesters;

    public CourseworkSheetApplicationService(
            CourseworkSheetRepository sheets,
            CourseworkSheetMapper mapper,
            SemesterRepository semesters
    ) {
        this.sheets = sheets;
        this.mapper = mapper;
        this.semesters = semesters;
    }

    @Transactional
    public void enterScore(long sheetId, TeacherID teacher, StudentID student, int score) {
        mutate(sheetId, sheet -> sheet.enterScore(teacher, student, score));
    }

    @Transactional
    public void close(long sheetId, TeacherID teacher) {
        mutate(sheetId, sheet -> sheet.close(teacher));
    }

    @Transactional
    public void reopen(long sheetId, DeanID dean, long semesterId) {
        CourseworkSheetJPAEntity entity = sheets.findDetailedById(sheetId)
                .orElseThrow(() -> new CourseworkSheetNotFoundException(
                        "Ведомость курсовой работы не найдена: " + sheetId
                ));
        var semester = semesters.findById(semesterId)
                .orElseThrow(() -> CatalogNotFoundException.semester(semesterId));
        var nextSemester = semesters.findFirstByStartDateAfterOrderByStartDateAsc(
                        entity.getCourse().getEndSemester().getEndDate())
                .orElseThrow(() -> new CatalogBadRequestException(
                        "Для курса не настроен следующий семестр пересдачи"
                ));
        if (!nextSemester.getId().equals(semester.getId())) {
            throw new CatalogBadRequestException("Ведомость курсовой работы можно переоткрыть только в следующем семестре");
        }
        CourseworkSheet sheet = mapper.toDomain(entity);
        sheet.reopen(dean);
        mapper.updateEntity(sheet, entity);
    }

    @Transactional
    public void archive(long sheetId, DeanID dean) {
        mutate(sheetId, sheet -> sheet.archive(dean));
    }

    private void mutate(long sheetId, Consumer<CourseworkSheet> operation) {
        CourseworkSheetJPAEntity entity = sheets.findDetailedById(sheetId)
                .orElseThrow(() -> new CourseworkSheetNotFoundException(
                        "Ведомость курсовой работы не найдена: " + sheetId
                ));
        CourseworkSheet sheet = mapper.toDomain(entity);
        operation.accept(sheet);
        mapper.updateEntity(sheet, entity);
    }
}
