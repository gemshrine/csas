CREATE TABLE groups (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(50)  NOT NULL,
    year_of_admission  INTEGER      NOT NULL,
    specialty         VARCHAR(100) NOT NULL,
    course            INTEGER      NOT NULL,
    education_form    VARCHAR(20)  NOT NULL,
    study_form        VARCHAR(20)  NOT NULL,
    created_at        TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_groups_name UNIQUE (name),
    CONSTRAINT ck_groups_education_form CHECK (education_form IN ('BUDGET', 'CONTRACT')),
    CONSTRAINT ck_groups_study_form CHECK (study_form IN ('FULL_TIME', 'PART_TIME', 'EXTRAMURAL'))
);
