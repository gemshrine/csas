CREATE TABLE coursework_sheets (
    id         BIGSERIAL PRIMARY KEY,
    course_id  BIGINT      NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_coursework_sheets_course_id UNIQUE (course_id),
    CONSTRAINT ck_coursework_sheets_status CHECK (status IN ('OPEN', 'CLOSED', 'ARCHIVED')),
    CONSTRAINT fk_coursework_sheets_course FOREIGN KEY (course_id) REFERENCES courses (id)
);

CREATE TABLE coursework_results (
    id                  BIGSERIAL PRIMARY KEY,
    coursework_sheet_id BIGINT  NOT NULL,
    student_id          BIGINT  NOT NULL,
    score               INTEGER NOT NULL,
    CONSTRAINT uk_coursework_results_sheet_student UNIQUE (coursework_sheet_id, student_id),
    CONSTRAINT ck_coursework_results_score CHECK (score BETWEEN 0 AND 100),
    CONSTRAINT fk_coursework_results_sheet
        FOREIGN KEY (coursework_sheet_id) REFERENCES coursework_sheets (id),
    CONSTRAINT fk_coursework_results_student FOREIGN KEY (student_id) REFERENCES students (id)
);

CREATE INDEX ix_coursework_results_student_id ON coursework_results (student_id);

INSERT INTO coursework_sheets (course_id)
SELECT id
FROM courses
WHERE course_type = 'COURSEWORK';
