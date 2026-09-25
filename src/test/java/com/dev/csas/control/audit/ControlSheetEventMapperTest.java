package com.dev.csas.control.audit;

import com.dev.csas.control.ControlResult;
import com.dev.csas.control.PassFail;
import com.dev.csas.control.entity.ControlSheetJPAEntity;
import com.dev.csas.shared.StudentID;
import com.dev.csas.student.StudentJPAEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ControlSheetEventMapperTest {

    private final ControlSheetEventMapper mapper = new ControlSheetEventMapper();
    private final ControlSheetJPAEntity sheet = ControlSheetJPAEntity.builder().id(5L).build();
    private final StudentJPAEntity student = StudentJPAEntity.builder().id(7L).build();
    private final Instant createdAt = Instant.parse("2026-09-19T18:00:00Z");

    @Test
    void mapsNumericResultChange() {
        ControlSheetEvent event = new ControlSheetEvent(
                ControlActorType.TEACHER,
                ControlEventType.RESULT_CHANGED,
                10L,
                new StudentID(7L),
                ControlResult.numeric(65),
                ControlResult.numeric(85),
                "Пересдача",
                createdAt
        );

        ControlSheetEventJPAEntity entity = mapper.toEntity(event, sheet, student);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getControlSheet()).isSameAs(sheet);
        assertThat(entity.getStudent()).isSameAs(student);
        assertThat(entity.getActorType()).isEqualTo(ControlActorType.TEACHER);
        assertThat(entity.getActorId()).isEqualTo(10L);
        assertThat(entity.getEventType()).isEqualTo(ControlEventType.RESULT_CHANGED);
        assertThat(entity.getOldNumericScore()).isEqualTo(65);
        assertThat(entity.getNewNumericScore()).isEqualTo(85);
        assertThat(entity.getOldPassFail()).isNull();
        assertThat(entity.getNewPassFail()).isNull();
        assertThat(entity.getReason()).isEqualTo("Пересдача");
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void mapsPassFailResultChange() {
        ControlSheetEvent event = new ControlSheetEvent(
                ControlActorType.TEACHER,
                ControlEventType.RESULT_CHANGED,
                10L,
                new StudentID(7L),
                ControlResult.passFail(PassFail.FAIL),
                ControlResult.passFail(PassFail.PASS),
                null,
                createdAt
        );

        ControlSheetEventJPAEntity entity = mapper.toEntity(event, sheet, student);

        assertThat(entity.getOldPassFail()).isEqualTo(PassFail.FAIL);
        assertThat(entity.getNewPassFail()).isEqualTo(PassFail.PASS);
        assertThat(entity.getOldNumericScore()).isNull();
        assertThat(entity.getNewNumericScore()).isNull();
    }

    @Test
    void mapsSheetEventWithoutStudentOrResults() {
        ControlSheetEvent event = new ControlSheetEvent(
                ControlActorType.DEAN,
                ControlEventType.REOPENED,
                30L,
                null,
                null,
                null,
                "Разрешена пересдача",
                createdAt
        );

        ControlSheetEventJPAEntity entity = mapper.toEntity(event, sheet, null);

        assertThat(entity.getControlSheet()).isSameAs(sheet);
        assertThat(entity.getStudent()).isNull();
        assertThat(entity.getActorType()).isEqualTo(ControlActorType.DEAN);
        assertThat(entity.getActorId()).isEqualTo(30L);
        assertThat(entity.getEventType()).isEqualTo(ControlEventType.REOPENED);
        assertThat(entity.getOldNumericScore()).isNull();
        assertThat(entity.getOldPassFail()).isNull();
        assertThat(entity.getNewNumericScore()).isNull();
        assertThat(entity.getNewPassFail()).isNull();
        assertThat(entity.getReason()).isEqualTo("Разрешена пересдача");
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void rejectsMissingEventOrSheet() {
        ControlSheetEvent event = new ControlSheetEvent(
                ControlActorType.TEACHER,
                ControlEventType.CLOSED,
                10L,
                null,
                null,
                null,
                null,
                createdAt
        );

        assertThatThrownBy(() -> mapper.toEntity(null, sheet, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toEntity(event, null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
