CREATE TABLE control_results (
    id               BIGSERIAL PRIMARY KEY,
    control_sheet_id BIGINT NOT NULL,
    student_id       BIGINT NOT NULL,
    numeric_score    INTEGER,
    pass_fail        VARCHAR(10),
    created_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_control_results_sheet_student UNIQUE (control_sheet_id, student_id),
    CONSTRAINT ck_control_results_pass_fail CHECK (pass_fail IS NULL OR pass_fail IN ('PASS', 'FAIL')),
    CONSTRAINT ck_control_results_value CHECK ((numeric_score IS NOT NULL) <> (pass_fail IS NOT NULL)),
    CONSTRAINT fk_control_results_control_sheet
        FOREIGN KEY (control_sheet_id) REFERENCES control_sheets (id),
    CONSTRAINT fk_control_results_student
        FOREIGN KEY (student_id) REFERENCES students (id)
);
