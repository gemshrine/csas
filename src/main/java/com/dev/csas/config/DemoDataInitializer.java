package com.dev.csas.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

@Component
@Profile("dev")
public class DemoDataInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final TransactionTemplate transactionTemplate;

    public DemoDataInitializer(
            JdbcTemplate jdbcTemplate,
            DataSource dataSource,
            TransactionTemplate transactionTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        transactionTemplate.executeWithoutResult(status -> {
            Boolean demoAccountExists = jdbcTemplate.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM users WHERE username = 'demo-dean')",
                    Boolean.class
            );
            if (!Boolean.TRUE.equals(demoAccountExists)) {
                new ResourceDatabasePopulator(new ClassPathResource("db/dev/demo-data.sql"))
                        .execute(dataSource);
            }
            new ResourceDatabasePopulator(new ClassPathResource("db/dev/coursework-demo-data.sql"))
                    .execute(dataSource);
        });
    }
}
