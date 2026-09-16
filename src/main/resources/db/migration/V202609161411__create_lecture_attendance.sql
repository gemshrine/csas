CREATE TABLE lecture_attendance (
    id          BIGSERIAL PRIMARY KEY,
    lecture_id  BIGINT  NOT NULL,
    student_id  BIGINT  NOT NULL,
    was_present BOOLEAN NOT NULL,
    created_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_lecture_attendance_lecture_student UNIQUE (lecture_id, student_id),
    CONSTRAINT fk_lecture_attendance_lecture
        FOREIGN KEY (lecture_id) REFERENCES lectures (id),
    CONSTRAINT fk_lecture_attendance_student
        FOREIGN KEY (student_id) REFERENCES students (id)
);
