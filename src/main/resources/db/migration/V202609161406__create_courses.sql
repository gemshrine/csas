CREATE TABLE courses (
    id                  BIGSERIAL PRIMARY KEY,
    discipline_id       BIGINT      NOT NULL,
    group_id            BIGINT      NOT NULL,
    semester_id         BIGINT      NOT NULL,
    seminar_teacher_id  BIGINT      NOT NULL,
    lecture_teacher_id  BIGINT      NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_courses_discipline_group_semester UNIQUE (discipline_id, group_id, semester_id),
    CONSTRAINT ck_courses_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
    CONSTRAINT fk_courses_discipline
        FOREIGN KEY (discipline_id) REFERENCES disciplines (id),
    CONSTRAINT fk_courses_group
        FOREIGN KEY (group_id) REFERENCES groups (id),
    CONSTRAINT fk_courses_semester
        FOREIGN KEY (semester_id) REFERENCES semesters (id),
    CONSTRAINT fk_courses_seminar_teacher
        FOREIGN KEY (seminar_teacher_id) REFERENCES teachers (id),
    CONSTRAINT fk_courses_lecture_teacher
        FOREIGN KEY (lecture_teacher_id) REFERENCES teachers (id)
);
