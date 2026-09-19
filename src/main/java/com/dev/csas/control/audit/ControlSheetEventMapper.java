package com.dev.csas.control.audit;

import com.dev.csas.control.ControlResult;
import com.dev.csas.control.entity.ControlSheetJPAEntity;
import com.dev.csas.student.StudentJPAEntity;

import java.util.Objects;

public class ControlSheetEventMapper {
    public ControlSheetEventJPAEntity toEntity(
            ControlSheetEvent event,
            ControlSheetJPAEntity sheet,
            StudentJPAEntity student
    ) {
        Objects.requireNonNull(event, "событие не может быть пустым");
        Objects.requireNonNull(sheet, "ведомость не может быть пустой");

        ControlResult oldResult = event.oldResult();
        ControlResult newResult = event.newResult();

        return ControlSheetEventJPAEntity.builder()
                .controlSheet(sheet)
                .student(student)
                .actorType(event.actorType())
                .actorId(event.actorId())
                .eventType(event.eventType())
                .oldNumericScore(oldResult == null ? null : oldResult.numericScore())
                .oldPassFail(oldResult == null ? null : oldResult.passFail())
                .newNumericScore(newResult == null ? null : newResult.numericScore())
                .newPassFail(newResult == null ? null : newResult.passFail())
                .reason(event.reason())
                .createdAt(event.createdAt())
                .build();
    }
}
