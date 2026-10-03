INSERT INTO users (username, password, role, enabled)
SELECT 'lab1-test-admin', '$2a$10$8rbpN.ld4ET11JDu/eVkmO487sLZultIPFYkm3aJdkde/FvtBe8K2', 'ADMIN', TRUE
UNION ALL
SELECT 'lab1-test-dean', '$2a$10$8rbpN.ld4ET11JDu/eVkmO487sLZultIPFYkm3aJdkde/FvtBe8K2', 'DEAN', TRUE
UNION ALL
SELECT format('lab1-test-teacher-%s', lpad(number::text, 2, '0')),
       '$2a$10$8rbpN.ld4ET11JDu/eVkmO487sLZultIPFYkm3aJdkde/FvtBe8K2',
       'TEACHER',
       TRUE
FROM generate_series(1, 10) AS series(number)
UNION ALL
SELECT format('lab1-test-student-%s', lpad(number::text, 2, '0')),
       '$2a$10$8rbpN.ld4ET11JDu/eVkmO487sLZultIPFYkm3aJdkde/FvtBe8K2',
       'STUDENT',
       TRUE
FROM generate_series(1, 20) AS series(number);

INSERT INTO teachers (first_name, last_name, department, position, academic_degree, email, phone, user_id)
SELECT 'Преподаватель',
       format('Тестовый %s', lpad(number::text, 2, '0')),
       format('Кафедра %s', lpad(((number - 1) % 5 + 1)::text, 2, '0')),
       'Преподаватель',
       CASE WHEN number % 3 = 0 THEN 'Кандидат наук' ELSE NULL END,
       format('teacher%s@example.test', lpad(number::text, 2, '0')),
       format('+700000000%s', lpad(number::text, 2, '0')),
       users.id
FROM generate_series(1, 10) AS series(number)
JOIN users ON users.username = format('lab1-test-teacher-%s', lpad(number::text, 2, '0'));

INSERT INTO groups (name, year_of_admission, specialty, course, education_form, study_form)
SELECT format('TEST-%s', lpad(number::text, 2, '0')),
       2022 + number,
       format('Специальность %s', lpad(number::text, 2, '0')),
       (number - 1) % 4 + 1,
       CASE WHEN number % 2 = 0 THEN 'CONTRACT' ELSE 'BUDGET' END,
       CASE WHEN number % 3 = 0 THEN 'PART_TIME' ELSE 'FULL_TIME' END
FROM generate_series(1, 5) AS series(number);

INSERT INTO students (
    first_name, last_name, patronymic, record_book_number, birth_date, email, phone,
    gender, education_form, study_form, admission_date, status, group_id, user_id
)
SELECT 'Студент',
       format('Тестовый %s', lpad(student_number::text, 2, '0')),
       'Тестович',
       format('TST%s', lpad(student_number::text, 7, '0')),
       DATE '2003-01-01' + student_number * 100,
       format('student%s@example.test', lpad(student_number::text, 2, '0')),
       format('+799900000%s', lpad(student_number::text, 2, '0')),
       CASE WHEN student_number % 2 = 0 THEN 'FEMALE' ELSE 'MALE' END,
       groups.education_form,
       groups.study_form,
       DATE '2022-09-01',
       'STUDYING',
       groups.id,
       users.id
FROM generate_series(1, 20) AS series(student_number)
JOIN groups ON groups.name = format('TEST-%s', lpad(((student_number - 1) / 4 + 1)::text, 2, '0'))
JOIN users ON users.username = format('lab1-test-student-%s', lpad(student_number::text, 2, '0'));

INSERT INTO disciplines (name, description, lecture_hours, practice_hours, lab_hours, control_form)
SELECT format('Тестовая дисциплина %s', lpad(number::text, 2, '0')),
       format('Учебная дисциплина для проверки, номер %s', number),
       16 + number,
       24 + number,
       8 + number,
       CASE number % 3
           WHEN 1 THEN 'EXAM'
           WHEN 2 THEN 'DIFF_CREDIT'
           ELSE 'CREDIT'
       END
FROM generate_series(1, 15) AS series(number);

INSERT INTO semesters (name, start_date, end_date)
VALUES
    ('Тестовый семестр 1', DATE '2025-09-01', DATE '2026-01-31'),
    ('Тестовый семестр 2', DATE '2026-02-01', DATE '2026-06-30'),
    ('Тестовый семестр 3', DATE '2026-09-01', DATE '2027-01-31'),
    ('Тестовый семестр 4', DATE '2027-02-01', DATE '2027-06-30');

WITH allocations AS (
    SELECT group_number,
           slot_number,
           (group_number - 1) * 3 + slot_number AS discipline_number,
           (group_number + slot_number - 2) % 3 + 1 AS first_semester
    FROM generate_series(1, 5) AS groups(group_number)
    CROSS JOIN generate_series(1, 3) AS slots(slot_number)
),
course_types AS (
    SELECT allocations.*,
           types.course_type,
           types.teacher_offset,
           CASE WHEN types.course_type = 'COURSEWORK' THEN first_semester + 1 ELSE first_semester END AS start_semester,
           first_semester + 1 AS end_semester
    FROM allocations
    CROSS JOIN (VALUES ('SEMINAR', 0), ('LECTURE', 3), ('COURSEWORK', 6)) AS types(course_type, teacher_offset)
)
INSERT INTO courses (
    discipline_id, group_id, start_semester_id, end_semester_id, course_type, teacher_id, status
)
SELECT disciplines.id,
       groups.id,
       start_term.id,
       end_term.id,
       course_types.course_type,
       teachers.id,
       CASE WHEN course_types.course_type = 'SEMINAR' THEN 'COMPLETED' ELSE 'IN_PROGRESS' END
FROM course_types
JOIN disciplines ON disciplines.name = format('Тестовая дисциплина %s', lpad(course_types.discipline_number::text, 2, '0'))
JOIN groups ON groups.name = format('TEST-%s', lpad(course_types.group_number::text, 2, '0'))
JOIN semesters start_term ON start_term.name = format('Тестовый семестр %s', course_types.start_semester)
JOIN semesters end_term ON end_term.name = format('Тестовый семестр %s', course_types.end_semester)
JOIN users teacher_user ON teacher_user.username = format(
    'lab1-test-teacher-%s',
    lpad(((course_types.group_number * 2 + course_types.slot_number + course_types.teacher_offset - 2) % 10 + 1)::text, 2, '0')
)
JOIN teachers ON teachers.user_id = teacher_user.id;

INSERT INTO course_students (course_id, student_id)
SELECT courses.id, students.id
FROM courses
JOIN groups ON groups.id = courses.group_id
JOIN students ON students.group_id = groups.id
WHERE groups.name LIKE 'TEST-%';

INSERT INTO course_works (course_id, number, title, max_score, work_date)
SELECT courses.id,
       work_number,
       format('Практическая работа %s', work_number),
       CASE work_number WHEN 1 THEN 5 WHEN 2 THEN 10 WHEN 3 THEN 15 ELSE 20 END,
       semesters.start_date + work_number * 14
FROM courses
JOIN semesters ON semesters.id = courses.start_semester_id
CROSS JOIN generate_series(1, 4) AS work_series(work_number)
WHERE courses.course_type = 'SEMINAR'
  AND courses.group_id IN (SELECT id FROM groups WHERE name LIKE 'TEST-%');

INSERT INTO course_scores (work_id, student_id, score)
SELECT course_works.id,
       course_students.student_id,
       (course_students.student_id + course_works.number * 3) % (course_works.max_score + 1)
FROM course_works
JOIN course_students ON course_students.course_id = course_works.course_id;

INSERT INTO control_sheets (course_id, control_form, status, grade_5_from, grade_4_from, grade_3_from)
SELECT courses.id, disciplines.control_form, 'OPEN', 85, 70, 50
FROM courses
JOIN disciplines ON disciplines.id = courses.discipline_id
WHERE courses.course_type = 'SEMINAR'
  AND courses.group_id IN (SELECT id FROM groups WHERE name LIKE 'TEST-%');

INSERT INTO control_results (control_sheet_id, student_id, numeric_score, pass_fail)
SELECT control_sheets.id,
       course_students.student_id,
       CASE WHEN control_sheets.control_form = 'CREDIT' THEN NULL
            ELSE 50 + (course_students.student_id + courses.id) % 51
       END,
       CASE WHEN control_sheets.control_form = 'CREDIT'
            THEN CASE WHEN (course_students.student_id + courses.id) % 4 = 0 THEN 'FAIL' ELSE 'PASS' END
            ELSE NULL
       END
FROM control_sheets
JOIN courses ON courses.id = control_sheets.course_id
JOIN course_students ON course_students.course_id = courses.id;

INSERT INTO attendance_sheets (course_id, status)
SELECT courses.id, 'OPEN'
FROM courses
WHERE courses.course_type = 'LECTURE'
  AND courses.group_id IN (SELECT id FROM groups WHERE name LIKE 'TEST-%');

INSERT INTO lectures (attendance_sheet_id, lecture_date, title)
SELECT attendance_sheets.id,
       semesters.start_date + lecture_number * 14,
       format('Лекция %s', lecture_number)
FROM attendance_sheets
JOIN courses ON courses.id = attendance_sheets.course_id
JOIN semesters ON semesters.id = courses.start_semester_id
CROSS JOIN generate_series(1, 2) AS lecture_series(lecture_number);

INSERT INTO lecture_attendance (lecture_id, student_id, was_present)
SELECT lectures.id,
       course_students.student_id,
       (course_students.student_id + lectures.id) % 4 <> 0
FROM lectures
JOIN attendance_sheets ON attendance_sheets.id = lectures.attendance_sheet_id
JOIN course_students ON course_students.course_id = attendance_sheets.course_id;

INSERT INTO coursework_sheets (course_id, status)
SELECT courses.id, 'OPEN'
FROM courses
WHERE courses.course_type = 'COURSEWORK'
  AND courses.group_id IN (SELECT id FROM groups WHERE name LIKE 'TEST-%');

INSERT INTO coursework_results (coursework_sheet_id, student_id, score)
SELECT coursework_sheets.id,
       course_students.student_id,
       CASE (RIGHT(users.username, 2)::INTEGER + disciplines.id) % 8
           WHEN 0 THEN 0
           WHEN 1 THEN 49
           WHEN 2 THEN 50
           WHEN 3 THEN 69
           WHEN 4 THEN 70
           WHEN 5 THEN 84
           WHEN 6 THEN 85
           ELSE 100
       END
FROM coursework_sheets
JOIN courses ON courses.id = coursework_sheets.course_id
JOIN disciplines ON disciplines.id = courses.discipline_id
JOIN course_students ON course_students.course_id = courses.id
JOIN students ON students.id = course_students.student_id
JOIN users ON users.id = students.user_id;
