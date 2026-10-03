ALTER TABLE courses
    DROP CONSTRAINT uk_courses_discipline_group_semester_type,
    DROP CONSTRAINT ck_courses_course_type;

ALTER TABLE courses
    ADD COLUMN start_semester_id BIGINT,
    ADD COLUMN end_semester_id BIGINT;

UPDATE courses
SET start_semester_id = semester_id,
    end_semester_id = semester_id;

ALTER TABLE courses
    ALTER COLUMN start_semester_id SET NOT NULL,
    ALTER COLUMN end_semester_id SET NOT NULL,
    DROP CONSTRAINT fk_courses_semester,
    DROP COLUMN semester_id,
    ADD CONSTRAINT ck_courses_course_type
        CHECK (course_type IN ('SEMINAR', 'LECTURE', 'COURSEWORK')),
    ADD CONSTRAINT fk_courses_start_semester
        FOREIGN KEY (start_semester_id) REFERENCES semesters (id),
    ADD CONSTRAINT fk_courses_end_semester
        FOREIGN KEY (end_semester_id) REFERENCES semesters (id),
    ADD CONSTRAINT uk_courses_discipline_group_period_type
        UNIQUE (discipline_id, group_id, start_semester_id, end_semester_id, course_type);
