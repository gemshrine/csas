CREATE TABLE control_sheet_events (
    id                 BIGSERIAL PRIMARY KEY,
    control_sheet_id   BIGINT NOT NULL,
    student_id         BIGINT,
    actor_type         VARCHAR(20) NOT NULL,
    actor_id           BIGINT NOT NULL,
    event_type         VARCHAR(30) NOT NULL,
    old_numeric_score  INTEGER,
    old_pass_fail      VARCHAR(10),
    new_numeric_score  INTEGER,
    new_pass_fail      VARCHAR(10),
    reason             VARCHAR(500),
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_control_sheet_events_sheet FOREIGN KEY (control_sheet_id) REFERENCES control_sheets (id),
    CONSTRAINT fk_control_sheet_events_student FOREIGN KEY (student_id) REFERENCES students (id),

    CONSTRAINT ck_control_sheet_events_actor_type CHECK (actor_type IN ('TEACHER', 'DEAN')),
    CONSTRAINT ck_control_sheet_events_event_type
        CHECK (event_type IN (
            'RESULT_ENTERED', 'RESULT_CHANGED',
            'REOPENED', 'CLOSED', 'ARCHIVED'
        )),

    CONSTRAINT ck_control_sheet_events_old_pass_fail CHECK (old_pass_fail IS NULL OR old_pass_fail IN ('PASS', 'FAIL')),
    CONSTRAINT ck_control_sheet_events_new_pass_fail CHECK (new_pass_fail IS NULL OR new_pass_fail IN ('PASS', 'FAIL'))
);

CREATE INDEX ix_control_sheet_events_sheet_created_at ON control_sheet_events (control_sheet_id, created_at);