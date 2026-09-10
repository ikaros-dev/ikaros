package run.ikaros.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
        Map<Long, Set<String>> filenamesByVersion = new HashMap<>();
        Arrays.stream(resources).forEach(resource -> {
            String filename = resource.getFilename();
            assertTrue(filename != null && MIGRATION_NAME.matcher(filename).matches(),
                () -> "invalid migration filename: " + filename);
            var matcher = MIGRATION_NAME.matcher(filename);
            matcher.matches();
            filenamesByVersion.computeIfAbsent(Long.parseLong(matcher.group(1)), ignored -> new HashSet<>())
                .add(filename);
        });
        filenamesByVersion.forEach((version, filenames) ->
            assertTrue(filenames.size() == 1,
                () -> "migration version " + version + " has multiple filenames: " + filenames));
        long[] versions = filenamesByVersion.keySet().stream()
            .mapToLong(Long::longValue)
            .sorted()
            .toArray();

        assertFalse(versions.length == 0, "the application must expose database migrations");
        for (int index = 1; index < versions.length; index++) {
            assertTrue(versions[index] > versions[index - 1], "migration versions must be unique and strictly increasing");
        }
    }

    private Resource[] migrationResources() throws IOException {
        Resource[] resources = new PathMatchingResourcePatternResolver()
            .getResources("classpath*:/db/migration/*.sql");
        Arrays.sort(resources, Comparator.comparing(Resource::getFilename));
        return resources;
    }
}
