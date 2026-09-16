CREATE TABLE teachers (
    id              BIGSERIAL PRIMARY KEY,
    first_name      VARCHAR(50)  NOT NULL,
    last_name       VARCHAR(50)  NOT NULL,
    patronymic      VARCHAR(50),
    department      VARCHAR(100) NOT NULL,
    position        VARCHAR(100) NOT NULL,
    academic_degree VARCHAR(50),
    email           VARCHAR(100),
    phone           VARCHAR(20),
    user_id         BIGINT,
    created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_teachers_user_id UNIQUE (user_id),
    CONSTRAINT fk_teachers_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);
