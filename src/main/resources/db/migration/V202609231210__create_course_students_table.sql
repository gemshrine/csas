CREATE TABLE course_students (
    course_id   BIGINT NOT NULL,
    student_id  BIGINT NOT NULL,
    CONSTRAINT pk_course_students PRIMARY KEY (course_id, student_id),
    CONSTRAINT fk_course_students_course
        FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE,
    CONSTRAINT fk_course_students_student
        FOREIGN KEY (student_id) REFERENCES students (id)
);

INSERT INTO course_students (course_id, student_id)
SELECT courses.id, students.id
FROM courses
JOIN students ON students.group_id = courses.group_id;
