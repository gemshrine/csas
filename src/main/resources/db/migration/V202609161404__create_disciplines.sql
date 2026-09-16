CREATE TABLE disciplines (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    description    VARCHAR(500),
    lecture_hours  INTEGER      NOT NULL,
    practice_hours INTEGER      NOT NULL,
    lab_hours      INTEGER      NOT NULL,
    control_form   VARCHAR(20)  NOT NULL,
    created_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_disciplines_name UNIQUE (name),
    CONSTRAINT ck_disciplines_control_form CHECK (control_form IN ('EXAM', 'DIFF_CREDIT', 'CREDIT'))
);
