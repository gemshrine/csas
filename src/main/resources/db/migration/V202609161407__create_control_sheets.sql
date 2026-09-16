CREATE TABLE control_sheets (
    id             BIGSERIAL PRIMARY KEY,
    course_id      BIGINT      NOT NULL,
    control_form   VARCHAR(20) NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    grade_5_from   INTEGER     NOT NULL,
    grade_4_from   INTEGER     NOT NULL,
    grade_3_from   INTEGER     NOT NULL,
    created_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_control_sheets_course_id UNIQUE (course_id),
    CONSTRAINT ck_control_sheets_control_form CHECK (control_form IN ('EXAM', 'DIFF_CREDIT', 'CREDIT')),
    CONSTRAINT ck_control_sheets_status CHECK (status IN ('OPEN', 'CLOSED', 'ARCHIVED')),
    CONSTRAINT fk_control_sheets_course
        FOREIGN KEY (course_id) REFERENCES courses (id)
);
