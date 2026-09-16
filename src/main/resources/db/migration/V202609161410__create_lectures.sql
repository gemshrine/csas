CREATE TABLE lectures (
    id                  BIGSERIAL PRIMARY KEY,
    attendance_sheet_id BIGINT       NOT NULL,
    lecture_date        DATE         NOT NULL,
    title               VARCHAR(200) NOT NULL,
    created_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lectures_attendance_sheet
        FOREIGN KEY (attendance_sheet_id) REFERENCES attendance_sheets (id)
);
