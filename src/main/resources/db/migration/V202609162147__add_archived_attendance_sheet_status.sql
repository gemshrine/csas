ALTER TABLE attendance_sheets
    DROP CONSTRAINT ck_attendance_sheets_status;

ALTER TABLE attendance_sheets
    ADD CONSTRAINT ck_attendance_sheets_status
        CHECK (status IN ('OPEN', 'CLOSED', 'ARCHIVED'));