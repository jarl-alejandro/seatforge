package com.jarl.seatforge.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:seatforge_flyway;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class FlywayStartupIntegrationTest {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    FlywayStartupIntegrationTest(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void flyway_creates_the_schema_before_hibernate_validates_it() {
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where version = '001' and success = true",
                Integer.class
        )).isEqualTo(1);

        assertThat(jdbcTemplate.queryForObject("select count(*) from events", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from tickets", Integer.class)).isZero();
    }
}
