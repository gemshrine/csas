CREATE TABLE attendance_sheets (
    id         BIGSERIAL PRIMARY KEY,
    course_id  BIGINT      NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_attendance_sheets_course_id UNIQUE (course_id),
    CONSTRAINT ck_attendance_sheets_status CHECK (status IN ('OPEN', 'CLOSED', 'ARCHIVED')),
    CONSTRAINT fk_attendance_sheets_course
        FOREIGN KEY (course_id) REFERENCES courses (id)
);
