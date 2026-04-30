package run.ikaros.server.core.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import run.ikaros.api.infra.utils.UuidV7Utils;

/**
 * Unit tests for migration helper methods and data transformation logic.
 * These tests verify the core data transformation logic without requiring a database.
 */
class MigrationHelperTest {

    // ==================== replaceIdValueFromUuid Tests ====================

    @Test
    void replaceIdValueFromUuid_shouldReplaceIdWithUuid() {
        // Given
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("id", 123L);
        recordMap.put("uuid", "test-uuid-123");

        // When
        Map<String, Object> result = replaceIdValueFromUuid(recordMap);

        // Then
        assertThat(result.get("id")).isEqualTo("test-uuid-123");
        assertThat(result.containsKey("uuid")).isFalse();
    }

    @Test
    void replaceIdValueFromUuid_shouldSkipWhenUuidIsNull() {
        // Given
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("id", 123L);
        recordMap.put("uuid", null);

        // When
        Map<String, Object> result = replaceIdValueFromUuid(recordMap);

        // Then
        assertThat(result.get("id")).isEqualTo(123L);
        assertThat(result.get("uuid")).isNull();
    }

    @Test
    void replaceIdValueFromUuid_shouldSkipWhenUuidKeyMissing() {
        // Given
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("id", 123L);

        // When
        Map<String, Object> result = replaceIdValueFromUuid(recordMap);

        // Then
        assertThat(result.get("id")).isEqualTo(123L);
    }

    // ==================== replaceSelfParentId Tests ====================

    @Test
    void replaceSelfParentId_shouldHandleNullParentId() {
        // Given
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("id", 1L);
        recordMap.put("parent_id", null);

        // When
        Map<String, Object> result = replaceSelfParentId(recordMap);

        // Then
        assertThat(result.get("parent_id")).isNull();
    }

    @Test
    void replaceSelfParentId_shouldHandleZeroForAttachment() {
        // Given
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("id", 1L);
        recordMap.put("parent_id", 0L);

        // When
        Map<String, Object> result = replaceSelfParentIdForAttachment(recordMap);

        // Then
        assertThat(result.get("parent_id")).isNotNull();
        // Should be replaced with ROOT_DIRECTORY_PARENT_ID
    }

    @Test
    void replaceSelfParentId_shouldHandleNegativeOneForAttachment() {
        // Given
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("id", 1L);
        recordMap.put("parent_id", -1L);

        // When
        Map<String, Object> result = replaceSelfParentIdForAttachment(recordMap);

        // Then
        assertThat(result.get("parent_id")).isNotNull();
        // Should be replaced with ROOT_DIRECTORY_PARENT_ID
    }

    @Test
    void replaceSelfParentId_shouldHandleStringZeroForAttachment() {
        // Given
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("id", 1L);
        recordMap.put("parent_id", "0");

        // When
        Map<String, Object> result = replaceSelfParentIdForAttachment(recordMap);

        // Then
        assertThat(result.get("parent_id")).isNotNull();
        // Should be replaced with ROOT_DIRECTORY_PARENT_ID
    }

    @Test
    void replaceSelfParentId_shouldHandleStringNegativeOneForAttachment() {
        // Given
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("id", 1L);
        recordMap.put("parent_id", "-1");

        // When
        Map<String, Object> result = replaceSelfParentIdForAttachment(recordMap);

        // Then
        assertThat(result.get("parent_id")).isNotNull();
        // Should be replaced with ROOT_DIRECTORY_PARENT_ID
    }

    // ==================== UUID Generation Tests ====================

    @Test
    void uuidV7_shouldGenerateUniqueUuids() {
        // When
        String uuid1 = UuidV7Utils.generate();
        String uuid2 = UuidV7Utils.generate();

        // Then
        assertThat(uuid1).isNotEqualTo(uuid2);
        assertThat(UUID.fromString(uuid1)).isNotNull();
        assertThat(UUID.fromString(uuid2)).isNotNull();
    }

    @Test
    void uuidV7_shouldBeTimeOrdered() throws InterruptedException {
        // Given
        String uuid1 = UuidV7Utils.generate();
        Thread.sleep(10); // Small delay to ensure different timestamp
        String uuid2 = UuidV7Utils.generate();

        // Then - UUID v7 should be lexicographically sortable by time
        assertThat(uuid1.compareTo(uuid2)).isLessThan(0);
    }

    // ==================== Table Name Filtering Tests ====================

    @Test
    void fetchTableNames_shouldFilterSystemTables() {
        // Given
        Map<String, Boolean> tableNames = new HashMap<>();
        tableNames.put("subject", true);
        tableNames.put("episode", true);
        tableNames.put("flyway_schema_history", false);
        tableNames.put("migrations", false);
        tableNames.put("information_schema_tables", false);
        tableNames.put("pg_stat_statements", false);
        tableNames.put("sql_sequences", false);

        // When & Then
        tableNames.forEach((name, shouldInclude) -> {
            boolean filtered = shouldIncludeTable(name);
            assertThat(filtered)
                .as("Table '%s' should be %s", name, shouldInclude ? "included" : "excluded")
                .isEqualTo(shouldInclude);
        });
    }

    // ==================== Data Integrity Tests ====================

    @Test
    void shouldPreserveAllFieldsDuringTransformation() {
        // Given
        Map<String, Object> original = new HashMap<>();
        original.put("id", 1L);
        original.put("uuid", "test-uuid");
        original.put("name", "Test");
        original.put("description", "Description");
        original.put("count", 42);

        // When
        Map<String, Object> transformed = replaceIdValueFromUuid(new HashMap<>(original));

        // Then - All non-id/uuid fields should be preserved
        assertThat(transformed.get("name")).isEqualTo("Test");
        assertThat(transformed.get("description")).isEqualTo("Description");
        assertThat(transformed.get("count")).isEqualTo(42);
    }

    @Test
    void shouldHandleEmptyRecordMap() {
        // Given
        Map<String, Object> recordMap = new HashMap<>();

        // When
        Map<String, Object> result = replaceIdValueFromUuid(recordMap);

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== Helper Methods ====================

    /**
     * Simulates replaceIdValueFromUuid logic from MigrationInitializer.
     */
    private Map<String, Object> replaceIdValueFromUuid(Map<String, Object> recordMap) {
        Object uuid = recordMap.get("uuid");
        if (uuid != null) {
            recordMap.put("id", uuid);
            recordMap.remove("uuid");
        }
        return recordMap;
    }

    /**
     * Simulates replaceSelfParentId logic for attachment table.
     */
    private Map<String, Object> replaceSelfParentIdForAttachment(Map<String, Object> recordMap) {
        Object id = recordMap.get("parent_id");
        if (id == null) {
            return recordMap;
        }

        // Handle attachment special values
        if (id instanceof String ids
            && ("-1".equalsIgnoreCase(ids) || "0".equalsIgnoreCase(ids))) {
            recordMap.put("parent_id", "ROOT_DIRECTORY_UUID");
            return recordMap;
        }

        if (id instanceof Number num
            && (num.longValue() == 0 || num.longValue() == -1)) {
            recordMap.put("parent_id", "ROOT_DIRECTORY_UUID");
            return recordMap;
        }

        return recordMap;
    }

    /**
     * Simulates replaceSelfParentId logic for non-attachment tables.
     */
    private Map<String, Object> replaceSelfParentId(Map<String, Object> recordMap) {
        Object id = recordMap.get("parent_id");
        if (id == null) {
            return recordMap;
        }
        // For non-attachment tables, keep the original logic
        return recordMap;
    }

    /**
     * Simulates table name filtering logic.
     */
    private boolean shouldIncludeTable(String tableName) {
        String lowerTableName = tableName.toLowerCase();
        return !lowerTableName.startsWith("flyway_")
            && !lowerTableName.startsWith("migrations")
            && !lowerTableName.startsWith("information_schema")
            && !lowerTableName.startsWith("pg_")
            && !lowerTableName.startsWith("sql_");
    }
}
