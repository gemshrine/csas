INSERT INTO courses (
    discipline_id, group_id, start_semester_id, end_semester_id, course_type, teacher_id, status
)
SELECT d.id, g.id, s.id, s.id, 'COURSEWORK', t.id, 'IN_PROGRESS'
FROM disciplines d
CROSS JOIN groups g
CROSS JOIN semesters s
CROSS JOIN teachers t
WHERE d.name = 'Демонстрационная дисциплина'
  AND g.name = 'DEMO-301'
  AND s.name = 'Демо семестр 2026'
  AND t.last_name = 'Семинарова'
  AND NOT EXISTS (
      SELECT 1 FROM courses existing
      WHERE existing.discipline_id = d.id
        AND existing.group_id = g.id
        AND existing.course_type = 'COURSEWORK'
  );

INSERT INTO course_students (course_id, student_id)
SELECT c.id, st.id
FROM courses c
CROSS JOIN students st
JOIN groups g ON g.id = c.group_id
WHERE c.discipline_id = (SELECT id FROM disciplines WHERE name = 'Демонстрационная дисциплина')
  AND g.name = 'DEMO-301'
  AND c.course_type = 'COURSEWORK'
  AND st.record_book_number = 'DEMO0001'
ON CONFLICT DO NOTHING;

UPDATE courses c
SET status = 'COMPLETED'
FROM disciplines d, groups g
WHERE c.discipline_id = d.id
  AND c.group_id = g.id
  AND d.name = 'Демонстрационная дисциплина'
  AND g.name = 'DEMO-301'
  AND c.course_type = 'COURSEWORK';

UPDATE courses c
SET status = 'COMPLETED'
FROM disciplines d, groups g
WHERE c.discipline_id = d.id
  AND c.group_id = g.id
  AND d.name = 'Демонстрационная дисциплина'
  AND g.name = 'DEMO-301'
  AND c.course_type = 'SEMINAR'
  AND EXISTS (SELECT 1 FROM control_sheets sheet WHERE sheet.course_id = c.id);

INSERT INTO coursework_sheets (course_id, status)
SELECT c.id, 'OPEN'
FROM courses c
JOIN groups g ON g.id = c.group_id
WHERE c.discipline_id = (SELECT id FROM disciplines WHERE name = 'Демонстрационная дисциплина')
  AND g.name = 'DEMO-301'
  AND c.course_type = 'COURSEWORK'
  AND NOT EXISTS (
      SELECT 1 FROM coursework_sheets sheet WHERE sheet.course_id = c.id
  );

INSERT INTO coursework_results (coursework_sheet_id, student_id, score)
SELECT sheet.id, st.id, 70
FROM coursework_sheets sheet
JOIN courses c ON c.id = sheet.course_id
CROSS JOIN students st
JOIN groups g ON g.id = c.group_id
WHERE c.discipline_id = (SELECT id FROM disciplines WHERE name = 'Демонстрационная дисциплина')
  AND g.name = 'DEMO-301'
  AND c.course_type = 'COURSEWORK'
  AND st.record_book_number = 'DEMO0001'
ON CONFLICT DO NOTHING;
