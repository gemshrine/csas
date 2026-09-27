INSERT INTO users (username, password, role, enabled)
VALUES
    ('demo-dean', '$2a$10$8rbpN.ld4ET11JDu/eVkmO487sLZultIPFYkm3aJdkde/FvtBe8K2', 'DEAN', TRUE),
    ('demo-seminar', '$2a$10$8rbpN.ld4ET11JDu/eVkmO487sLZultIPFYkm3aJdkde/FvtBe8K2', 'TEACHER', TRUE),
    ('demo-lecture', '$2a$10$8rbpN.ld4ET11JDu/eVkmO487sLZultIPFYkm3aJdkde/FvtBe8K2', 'TEACHER', TRUE),
    ('demo-student', '$2a$10$8rbpN.ld4ET11JDu/eVkmO487sLZultIPFYkm3aJdkde/FvtBe8K2', 'STUDENT', TRUE);

INSERT INTO teachers (first_name, last_name, department, position, user_id)
SELECT 'Анна', 'Семинарова', 'Информатика', 'Преподаватель', id
FROM users WHERE username = 'demo-seminar';

INSERT INTO teachers (first_name, last_name, department, position, user_id)
SELECT 'Иван', 'Лекторов', 'Информатика', 'Преподаватель', id
FROM users WHERE username = 'demo-lecture';

INSERT INTO groups (name, year_of_admission, specialty, course, education_form, study_form)
VALUES ('DEMO-301', 2024, 'Программная инженерия', 3, 'BUDGET', 'FULL_TIME');

INSERT INTO students (first_name, last_name, patronymic, record_book_number, group_id, user_id)
SELECT 'Алексей', 'Тестов', 'Демо', 'DEMO0001', g.id, u.id
FROM groups g CROSS JOIN users u
WHERE g.name = 'DEMO-301' AND u.username = 'demo-student';

INSERT INTO disciplines (name, lecture_hours, practice_hours, lab_hours, control_form)
VALUES ('Демонстрационная дисциплина', 16, 32, 16, 'EXAM');

INSERT INTO semesters (name, start_date, end_date)
VALUES ('Демо семестр 2026', DATE '2026-09-01', DATE '2027-01-31');

INSERT INTO courses (discipline_id, group_id, semester_id, seminar_teacher_id, lecture_teacher_id, status)
SELECT d.id, g.id, s.id, ts.id, tl.id, 'IN_PROGRESS'
FROM disciplines d
CROSS JOIN groups g
CROSS JOIN semesters s
CROSS JOIN teachers ts
CROSS JOIN teachers tl
WHERE d.name = 'Демонстрационная дисциплина'
  AND g.name = 'DEMO-301'
  AND s.name = 'Демо семестр 2026'
  AND ts.last_name = 'Семинарова'
  AND tl.last_name = 'Лекторов';

INSERT INTO course_students (course_id, student_id)
SELECT c.id, st.id
FROM courses c
CROSS JOIN students st
WHERE st.record_book_number = 'DEMO0001'
  AND c.discipline_id = (SELECT id FROM disciplines WHERE name = 'Демонстрационная дисциплина');

INSERT INTO course_works (course_id, number, title, max_score, work_date)
SELECT id, 1, 'Демонстрационная работа', 20, DATE '2026-10-01'
FROM courses
WHERE discipline_id = (SELECT id FROM disciplines WHERE name = 'Демонстрационная дисциплина');

INSERT INTO course_scores (work_id, student_id, score)
SELECT w.id, st.id, 16
FROM course_works w
CROSS JOIN students st
WHERE w.title = 'Демонстрационная работа'
  AND st.record_book_number = 'DEMO0001';

INSERT INTO control_sheets (course_id, control_form, status, grade_5_from, grade_4_from, grade_3_from)
SELECT id, 'EXAM', 'OPEN', 90, 80, 70
FROM courses
WHERE discipline_id = (SELECT id FROM disciplines WHERE name = 'Демонстрационная дисциплина');

INSERT INTO control_results (control_sheet_id, student_id, numeric_score)
SELECT cs.id, st.id, 84
FROM control_sheets cs
CROSS JOIN students st
WHERE st.record_book_number = 'DEMO0001'
  AND cs.course_id = (SELECT id FROM courses WHERE discipline_id =
      (SELECT id FROM disciplines WHERE name = 'Демонстрационная дисциплина'));

INSERT INTO attendance_sheets (course_id, status)
SELECT id, 'OPEN'
FROM courses
WHERE discipline_id = (SELECT id FROM disciplines WHERE name = 'Демонстрационная дисциплина');

INSERT INTO lectures (attendance_sheet_id, lecture_date, title)
SELECT id, DATE '2026-09-15', 'Вводная лекция'
FROM attendance_sheets
WHERE course_id = (SELECT id FROM courses WHERE discipline_id =
    (SELECT id FROM disciplines WHERE name = 'Демонстрационная дисциплина'));

INSERT INTO lecture_attendance (lecture_id, student_id, was_present)
SELECT l.id, st.id, TRUE
FROM lectures l
CROSS JOIN students st
WHERE l.title = 'Вводная лекция'
  AND st.record_book_number = 'DEMO0001';
