package com.dev.csas.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
class DevTestDataMigrationIntegrationTest {

    @Container
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("csas_dev_seed_test")
            .withUsername("csas")
            .withPassword("csas");

    @Test
    void seedsTestDataWithoutDuplicatingExistingDemoData() {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        DataSource dataSource = new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        new ResourceDatabasePopulator(new ClassPathResource("db/dev/demo-data.sql")).execute(dataSource);
        new ResourceDatabasePopulator(new ClassPathResource("db/dev/coursework-demo-data.sql")).execute(dataSource);

        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration", "classpath:db/dev/migration")
                .outOfOrder(true)
                .load()
                .migrate();

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        assertEquals(1, count(jdbc, """
                SELECT COUNT(*) FROM course_scores
                WHERE student_id = (SELECT id FROM students WHERE record_book_number = 'DEMO0001')
                """));
        assertEquals(1, count(jdbc, """
                SELECT COUNT(*) FROM control_results
                WHERE student_id = (SELECT id FROM students WHERE record_book_number = 'DEMO0001')
                """));
        assertEquals(1, count(jdbc, "SELECT COUNT(*) FROM lectures WHERE title = 'Вводная лекция'"));
        assertEquals(1, count(jdbc, """
                SELECT COUNT(*) FROM lecture_attendance
                WHERE student_id = (SELECT id FROM students WHERE record_book_number = 'DEMO0001')
                """));
        assertEquals(1, count(jdbc, """
                SELECT COUNT(*) FROM coursework_results
                WHERE student_id = (SELECT id FROM students WHERE record_book_number = 'DEMO0001')
                """));
        assertEquals(20, count(jdbc, "SELECT COUNT(*) FROM students WHERE record_book_number LIKE 'TST%'"));
        assertEquals(5, count(jdbc, "SELECT COUNT(*) FROM groups WHERE name LIKE 'TEST-%'"));
        assertEquals(1, count(jdbc, """
                SELECT COUNT(*)
                FROM groups
                JOIN users ON users.id = groups.curator_id
                WHERE groups.name = 'DEMO-301' AND users.username = 'demo-dean'
                """));
        assertEquals(5, count(jdbc, """
                SELECT COUNT(*)
                FROM groups
                JOIN users ON users.id = groups.curator_id
                WHERE groups.name LIKE 'TEST-%' AND users.username = 'lab1-test-dean'
                """));
        assertEquals(15, count(jdbc, "SELECT COUNT(*) FROM disciplines WHERE name LIKE 'Тестовая дисциплина %'"));
        assertEquals(10, count(jdbc, """
                SELECT COUNT(*)
                FROM teachers
                JOIN users ON users.id = teachers.user_id
                WHERE users.username LIKE 'lab1-test-teacher-%'
                """));
        assertEquals(240, count(jdbc, """
                SELECT COUNT(*)
                FROM course_scores
                JOIN course_works ON course_works.id = course_scores.work_id
                JOIN courses ON courses.id = course_works.course_id
                JOIN disciplines ON disciplines.id = courses.discipline_id
                WHERE disciplines.name LIKE 'Тестовая дисциплина %'
                """));
        assertEquals(45, count(jdbc, """
                SELECT COUNT(*)
                FROM courses
                WHERE group_id IN (SELECT id FROM groups WHERE name LIKE 'TEST-%')
                """));
        assertEquals(15, count(jdbc, """
                SELECT COUNT(*)
                FROM control_sheets
                WHERE course_id IN (
                    SELECT id FROM courses
                    WHERE course_type = 'SEMINAR'
                      AND group_id IN (SELECT id FROM groups WHERE name LIKE 'TEST-%')
                )
                """));
        assertEquals(5, count(jdbc, """
                SELECT COUNT(*)
                FROM courses
                WHERE course_type = 'SEMINAR'
                  AND status = 'IN_PROGRESS'
                  AND group_id IN (SELECT id FROM groups WHERE name LIKE 'TEST-%')
                """));
        assertEquals(10, count(jdbc, """
                SELECT COUNT(*)
                FROM courses
                WHERE course_type = 'SEMINAR'
                  AND status = 'COMPLETED'
                  AND group_id IN (SELECT id FROM groups WHERE name LIKE 'TEST-%')
                """));
        assertEquals(5, count(jdbc, """
                SELECT COUNT(*) FROM (
                    SELECT group_id
                    FROM courses
                    WHERE course_type = 'SEMINAR'
                      AND status = 'IN_PROGRESS'
                      AND group_id IN (SELECT id FROM groups WHERE name LIKE 'TEST-%')
                    GROUP BY group_id
                ) open_courses_per_group
                """));
        assertEquals(60, count(jdbc, """
                SELECT COUNT(*)
                FROM coursework_results
                WHERE coursework_sheet_id IN (
                    SELECT id FROM coursework_sheets
                    WHERE course_id IN (
                        SELECT id FROM courses
                        WHERE course_type = 'COURSEWORK'
                          AND group_id IN (SELECT id FROM groups WHERE name LIKE 'TEST-%')
                    )
                )
                """));
    }

    private int count(JdbcTemplate jdbc, String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }
}
