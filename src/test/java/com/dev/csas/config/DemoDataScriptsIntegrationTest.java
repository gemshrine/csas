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
class DemoDataScriptsIntegrationTest {

    @Container
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("csas_demo_data_test")
            .withUsername("csas")
            .withPassword("csas");

    @Test
    void createsCourseTypesAndAppliesSupplementalSeedIdempotently() {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        DataSource dataSource = new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        ResourceDatabasePopulator mainSeed = new ResourceDatabasePopulator(
                new ClassPathResource("db/dev/demo-data.sql"));
        ResourceDatabasePopulator courseworkSeed = new ResourceDatabasePopulator(
                new ClassPathResource("db/dev/coursework-demo-data.sql"));
        mainSeed.execute(dataSource);
        courseworkSeed.execute(dataSource);
        courseworkSeed.execute(dataSource);

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertEquals(1, count(jdbc, "SEMINAR"));
        assertEquals(1, count(jdbc, "LECTURE"));
        assertEquals(1, count(jdbc, "COURSEWORK"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM control_sheets", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM attendance_sheets", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM coursework_sheets", Integer.class));
        assertEquals(70, jdbc.queryForObject("SELECT score FROM coursework_results", Integer.class));
        assertEquals("COMPLETED", jdbc.queryForObject(
                "SELECT status FROM courses WHERE course_type = 'SEMINAR'", String.class));
        assertEquals("COMPLETED", jdbc.queryForObject(
                "SELECT status FROM courses WHERE course_type = 'COURSEWORK'", String.class));
    }

    private int count(JdbcTemplate jdbc, String courseType) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM courses WHERE course_type = ?", Integer.class, courseType);
    }
}
