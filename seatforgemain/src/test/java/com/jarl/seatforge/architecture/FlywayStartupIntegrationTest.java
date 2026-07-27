package com.jarl.seatforge.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class FlywayStartupIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    FlywayStartupIntegrationTest(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void flyway_creates_the_schema_before_hibernate_validates_it() {
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where version = '003' and success = true",
                Integer.class
        )).isEqualTo(1);

        assertThat(jdbcTemplate.queryForObject("select count(*) from events", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from tickets", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from reservations", Integer.class)).isZero();
    }
}
