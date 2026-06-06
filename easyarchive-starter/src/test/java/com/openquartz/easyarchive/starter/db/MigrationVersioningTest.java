package com.openquartz.easyarchive.starter.db;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationVersioningTest {

    private static final Path MIGRATION_DIR = Path.of("src/main/resources/db/migration");

    @Test
    void versionedMigrationsShouldUseUniqueVersions() throws IOException {
        List<String> duplicateVersions = Files.list(MIGRATION_DIR)
                .map(Path::getFileName)
                .map(Path::toString)
                .filter(name -> name.matches("^V\\d+__.+\\.sql$"))
                .collect(Collectors.groupingBy(MigrationVersioningTest::extractVersion, HashMap::new, Collectors.toList()))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());

        assertTrue(duplicateVersions.isEmpty(), "duplicate migration versions: " + duplicateVersions);
    }

    @Test
    void shouldContainVersion9InAppNotificationMigration() throws IOException {
        List<String> migrationFiles = Files.list(MIGRATION_DIR)
                .map(Path::getFileName)
                .map(Path::toString)
                .collect(Collectors.toList());

        assertFalse(migrationFiles.isEmpty(), "expected migration directory to contain files");
        assertTrue(migrationFiles.contains("V9__add_in_app_notifications.sql"),
                "expected V9 in-app notification migration to exist");
    }

    private static String extractVersion(String fileName) {
        return fileName.substring(0, fileName.indexOf("__"));
    }
}
