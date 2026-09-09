package run.ikaros.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class DatabaseInitializationContractTest {
    private static final Pattern MIGRATION_NAME = Pattern.compile("V(\\d+)__([A-Za-z0-9_]+)\\.sql");

    @Test
    void discoversEveryOwnerMigrationFromTheAggregatedClasspath() throws IOException {
        Resource[] resources = migrationResources();

        assertTrue(resources.length > 0, "the application must expose database migrations");
        Map<String, byte[]> contents = new HashMap<>();
        Arrays.stream(resources).forEach(resource -> {
            String filename = resource.getFilename();
            assertTrue(filename != null && MIGRATION_NAME.matcher(filename).matches(),
                () -> "invalid migration filename: " + filename);
            try {
                byte[] bytes = resource.getInputStream().readAllBytes();
                byte[] previous = contents.putIfAbsent(filename, bytes);
                assertTrue(previous == null || Arrays.equals(previous, bytes),
                    () -> "conflicting migration content: " + filename);
            } catch (IOException exception) {
                throw new IllegalStateException("cannot read migration: " + filename, exception);
            }
        });
    }

    @Test
    void migrationVersionsAreStrictlyIncreasing() throws IOException {
        Resource[] resources = migrationResources();
        long[] versions = Arrays.stream(resources)
            .map(Resource::getFilename)
            .map(MIGRATION_NAME::matcher)
            .peek(matcher -> assertTrue(matcher.matches()))
            .mapToLong(matcher -> Long.parseLong(matcher.group(1)))
            .distinct()
            .sorted()
            .toArray();

        assertFalse(versions.length == 0, "the application must expose database migrations");
        for (int index = 1; index < versions.length; index++) {
            assertNotEquals(versions[index - 1], versions[index], "migration versions must be unique");
        }
    }

    private Resource[] migrationResources() throws IOException {
        Resource[] resources = new PathMatchingResourcePatternResolver()
            .getResources("classpath*:/db/migration/*.sql");
        Arrays.sort(resources, Comparator.comparing(Resource::getFilename));
        return resources;
    }
}
