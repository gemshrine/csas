CREATE TABLE students (
    id                  BIGSERIAL PRIMARY KEY,
    first_name          VARCHAR(50) NOT NULL,
    last_name           VARCHAR(50) NOT NULL,
    patronymic          VARCHAR(50),
    record_book_number  VARCHAR(10) NOT NULL,
    birth_date          DATE,
    email               VARCHAR(100),
    phone               VARCHAR(20),
    gender              VARCHAR(10),
    education_form      VARCHAR(20),
    study_form          VARCHAR(20),
    admission_date      DATE,
    status              VARCHAR(20),
    group_id            BIGINT,
    user_id             BIGINT,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_students_record_book_number UNIQUE (record_book_number),
    CONSTRAINT uk_students_user_id UNIQUE (user_id),
    CONSTRAINT ck_students_gender CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE')),
    CONSTRAINT ck_students_education_form CHECK (education_form IS NULL OR education_form IN ('BUDGET', 'CONTRACT')),
    CONSTRAINT ck_students_study_form CHECK (study_form IS NULL OR study_form IN ('FULL_TIME', 'PART_TIME', 'EXTRAMURAL')),
    CONSTRAINT ck_students_status CHECK (status IS NULL OR status IN ('STUDYING', 'ACADEMIC_LEAVE', 'EXPELLED')),
    CONSTRAINT fk_students_group
        FOREIGN KEY (group_id) REFERENCES groups (id),
    CONSTRAINT fk_students_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);
