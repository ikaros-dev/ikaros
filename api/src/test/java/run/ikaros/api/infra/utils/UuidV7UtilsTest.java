package run.ikaros.api.infra.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidV7UtilsTest {

    @Test
    void generate_ShouldReturnNonNullString() {
        String uuid = UuidV7Utils.generate();
        assertNotNull(uuid);
        assertFalse(uuid.isEmpty());
    }

    @Test
    void generate_ShouldReturnValidUuid() {
        String uuid = UuidV7Utils.generate();
        assertDoesNotThrow(() -> UUID.fromString(uuid));
    }

    @Test
    void generate_ShouldReturnUniqueUuids() {
        Set<String> uuids = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            String uuid = UuidV7Utils.generate();
            assertTrue(uuids.add(uuid), "UUID should be unique");
        }
        assertEquals(100, uuids.size());
    }

    @Test
    void generateUuid_ShouldReturnNonNullUuid() {
        UUID uuid = UuidV7Utils.generateUuid();
        assertNotNull(uuid);
    }

    @Test
    void generateUuid_ShouldReturnUniqueUuids() {
        Set<UUID> uuids = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            UUID uuid = UuidV7Utils.generateUuid();
            assertTrue(uuids.add(uuid), "UUID should be unique");
        }
        assertEquals(100, uuids.size());
    }

    @Test
    void generate_ShouldHaveCorrectLength() {
        String uuid = UuidV7Utils.generate();
        // UUID string format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx (36 characters)
        assertEquals(36, uuid.length());
    }

    @Test
    void generate_ShouldHaveCorrectFormat() {
        String uuid = UuidV7Utils.generate();
        // UUID format: 8-4-4-4-12 hex digits separated by hyphens
        assertTrue(uuid.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"));
    }
}