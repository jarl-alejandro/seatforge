package com.jarl.seatforge.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationNamingTest {

    private static final Pattern VERSIONED_MIGRATION =
            Pattern.compile("V\\d{3}__[a-z][a-z0-9_]*\\.sql");

    @Test
    void migrations_follow_the_project_flyway_naming_convention() throws IOException {
        Path migrationDirectory = Path.of("src/main/resources/db/migration");

        List<String> migrationNames;
        try (var files = Files.list(migrationDirectory)) {
            migrationNames = files
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
        }

        assertThat(migrationNames)
                .isNotEmpty()
                .allMatch(name -> VERSIONED_MIGRATION.matcher(name).matches(),
                        "usar V###__descripcion_en_snake_case.sql");
    }
}
