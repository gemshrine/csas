WITH numbered_seminar_courses AS (
    SELECT courses.id,
           ROW_NUMBER() OVER (PARTITION BY courses.group_id ORDER BY disciplines.name) AS course_number
    FROM courses
    JOIN groups ON groups.id = courses.group_id
    JOIN disciplines ON disciplines.id = courses.discipline_id
    WHERE courses.course_type = 'SEMINAR'
      AND groups.name LIKE 'TEST-%'
)
UPDATE courses
SET status = 'IN_PROGRESS'
FROM numbered_seminar_courses
WHERE courses.id = numbered_seminar_courses.id
  AND numbered_seminar_courses.course_number = 1;
