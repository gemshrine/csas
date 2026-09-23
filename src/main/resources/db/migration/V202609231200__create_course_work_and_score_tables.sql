CREATE TABLE course_works (
    id          BIGSERIAL PRIMARY KEY,
    course_id   BIGINT      NOT NULL,
    number      BIGINT      NOT NULL,
    title       TEXT        NOT NULL,
    max_score   INTEGER     NOT NULL,
    work_date   DATE        NOT NULL,
    CONSTRAINT uk_course_works_course_number UNIQUE (course_id, number),
    CONSTRAINT ck_course_works_number_positive CHECK (number > 0),
    CONSTRAINT ck_course_works_max_score_positive CHECK (max_score > 0),
    CONSTRAINT fk_course_works_course
        FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
);

CREATE TABLE course_scores (
    id          BIGSERIAL PRIMARY KEY,
    work_id     BIGINT  NOT NULL,
    student_id  BIGINT  NOT NULL,
    score       INTEGER NOT NULL,
    CONSTRAINT uk_course_scores_work_student UNIQUE (work_id, student_id),
    CONSTRAINT ck_course_scores_score_non_negative CHECK (score >= 0),
    CONSTRAINT fk_course_scores_work
        FOREIGN KEY (work_id) REFERENCES course_works (id) ON DELETE CASCADE,
    CONSTRAINT fk_course_scores_student
        FOREIGN KEY (student_id) REFERENCES students (id)
);
