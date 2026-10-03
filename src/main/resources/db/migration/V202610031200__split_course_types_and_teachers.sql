ALTER TABLE courses
    DROP CONSTRAINT uk_courses_discipline_group_semester;

ALTER TABLE courses
    ADD COLUMN course_type VARCHAR(20),
    ADD COLUMN teacher_id BIGINT;

UPDATE courses
SET course_type = 'SEMINAR',
    teacher_id = seminar_teacher_id;

INSERT INTO courses (
    discipline_id,
    group_id,
    semester_id,
    seminar_teacher_id,
    lecture_teacher_id,
    course_type,
    teacher_id,
    status,
    created_at,
    updated_at
)
SELECT discipline_id,
       group_id,
       semester_id,
       seminar_teacher_id,
       lecture_teacher_id,
       'LECTURE',
       lecture_teacher_id,
       'IN_PROGRESS',
       created_at,
       updated_at
FROM courses;

INSERT INTO course_students (course_id, student_id)
SELECT lecture.id, enrollment.student_id
FROM courses seminar
JOIN courses lecture
  ON lecture.discipline_id = seminar.discipline_id
 AND lecture.group_id = seminar.group_id
 AND lecture.semester_id = seminar.semester_id
 AND lecture.course_type = 'LECTURE'
JOIN course_students enrollment ON enrollment.course_id = seminar.id
WHERE seminar.course_type = 'SEMINAR';

UPDATE attendance_sheets attendance
SET course_id = lecture.id
FROM courses seminar
JOIN courses lecture
  ON lecture.discipline_id = seminar.discipline_id
 AND lecture.group_id = seminar.group_id
 AND lecture.semester_id = seminar.semester_id
 AND lecture.course_type = 'LECTURE'
WHERE attendance.course_id = seminar.id
  AND seminar.course_type = 'SEMINAR';

ALTER TABLE courses
    DROP CONSTRAINT fk_courses_seminar_teacher,
    DROP CONSTRAINT fk_courses_lecture_teacher,
    DROP COLUMN seminar_teacher_id,
    DROP COLUMN lecture_teacher_id,
    ALTER COLUMN course_type SET NOT NULL,
    ALTER COLUMN teacher_id SET NOT NULL,
    ADD CONSTRAINT ck_courses_course_type CHECK (course_type IN ('SEMINAR', 'LECTURE')),
    ADD CONSTRAINT fk_courses_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id),
    ADD CONSTRAINT uk_courses_discipline_group_semester_type
        UNIQUE (discipline_id, group_id, semester_id, course_type);

CREATE INDEX ix_courses_teacher_id ON courses (teacher_id);
