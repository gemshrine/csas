package com.dev.csas.attendance;

import com.dev.csas.audit.UserActionService;
import com.dev.csas.audit.UserActionType;
import com.dev.csas.shared.DeanID;
import com.dev.csas.shared.LectureID;
import com.dev.csas.shared.StudentID;
import com.dev.csas.shared.TeacherID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Consumer;

@Service
public class AttendanceSheetApplicationService {

    private final AttendanceSheetRepository sheets;
    private final AttendanceSheetMapper mapper;
    private final UserActionService userActions;

    public AttendanceSheetApplicationService(
            AttendanceSheetRepository sheets,
            AttendanceSheetMapper mapper,
            UserActionService userActions
    ) {
        this.sheets = sheets;
        this.mapper = mapper;
        this.userActions = userActions;
    }

    @Transactional
    public LectureID addLecture(long sheetId, TeacherID teacher, LocalDate date, String title) {
        AttendanceSheetJPAEntity entity = findSheet(sheetId);
        AttendanceSheet sheet = mapper.toDomain(entity);

        LectureJPAEntity lecture = LectureJPAEntity.builder()
                .attendanceSheet(entity)
                .lectureDate(date)
                .title(title)
                .build();
        entity.getLectures().add(lecture);
        sheets.flush();

        LectureID lectureID = new LectureID(Objects.requireNonNull(lecture.getId()));
        sheet.addLecture(teacher, lectureID, date, title);
        mapper.updateEntity(sheet, entity);
        userActions.record(UserActionType.ATTENDANCE_LECTURE_ADDED, "ATTENDANCE_SHEET", sheetId,
                "Идентификатор лекции: " + lectureID.id());
        return lectureID;
    }

    @Transactional
    public void registerAttendance(
            long sheetId,
            TeacherID teacher,
            StudentID student,
            AttendanceStatusEnum status,
            LectureID lecture
    ) {
        mutate(sheetId, UserActionType.ATTENDANCE_REGISTERED,
                "Идентификатор лекции: " + lecture.id() + ", идентификатор студента: " + student.id(),
                sheet -> sheet.register(teacher, student, status, lecture));
    }

    @Transactional
    public void close(long sheetId, TeacherID teacher) {
        mutate(sheetId, UserActionType.ATTENDANCE_SHEET_CLOSED, null, sheet -> sheet.close(teacher));
    }

    @Transactional
    public void archive(long sheetId, DeanID dean) {
        mutate(sheetId, UserActionType.ATTENDANCE_SHEET_ARCHIVED, null, sheet -> sheet.archive(dean));
    }

    private void mutate(long sheetId, UserActionType actionType, String details, Consumer<AttendanceSheet> operation) {
        AttendanceSheetJPAEntity entity = findSheet(sheetId);
        AttendanceSheet sheet = mapper.toDomain(entity);
        operation.accept(sheet);
        mapper.updateEntity(sheet, entity);
        userActions.record(actionType, "ATTENDANCE_SHEET", sheetId, details);
    }

    private AttendanceSheetJPAEntity findSheet(long sheetId) {
        return sheets.findById(sheetId)
                .orElseThrow(() -> new AttendanceSheetNotFoundException(
                        "Ведомость посещаемости не найдена: " + sheetId
                ));
    }
}
