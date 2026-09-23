package com.dev.csas.control.audit;

import com.dev.csas.control.ControlResult;
import com.dev.csas.shared.StudentID;

import java.time.Instant;

public record ControlSheetEvent(
        ControlActorType actorType,
        ControlEventType eventType,
        long actorId,
        StudentID studentID,
        ControlResult oldResult,
        ControlResult newResult,
        String reason,
        Instant createdAt
) {
}
