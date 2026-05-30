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
import reactor.test.StepVerifier;

class ReactiveUuidV7UtilsTest {

    @Test
    void generate_ShouldReturnValidUuidString() {
        StepVerifier.create(ReactiveUuidV7Utils.generate())
            .assertNext(uuid -> {
                assertNotNull(uuid);
                assertFalse(uuid.isEmpty());
                assertEquals(36, uuid.length());
                assertDoesNotThrow(() -> UUID.fromString(uuid));
            })
            .verifyComplete();
    }

    @Test
    void generate_ShouldReturnUniqueUuids() {
        Set<String> uuids = new HashSet<>();
        
        for (int i = 0; i < 100; i++) {
            StepVerifier.create(ReactiveUuidV7Utils.generate())
                .assertNext(uuid -> assertTrue(uuids.add(uuid), "UUID should be unique"))
                .verifyComplete();
        }
        
        assertEquals(100, uuids.size());
    }

    @Test
    void generate_ShouldHaveCorrectFormat() {
        StepVerifier.create(ReactiveUuidV7Utils.generate())
            .assertNext(uuid -> {
                assertTrue(uuid.matches(
                    "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"));
            })
            .verifyComplete();
    }

    @Test
    void generateUuid_ShouldReturnValidUuid() {
        StepVerifier.create(ReactiveUuidV7Utils.generateUuid())
            .assertNext(uuid -> {
                assertNotNull(uuid);
                assertDoesNotThrow(() -> UUID.fromString(uuid.toString()));
            })
            .verifyComplete();
    }

    @Test
    void generateUuid_ShouldReturnUniqueUuids() {
        Set<UUID> uuids = new HashSet<>();
        
        for (int i = 0; i < 100; i++) {
            StepVerifier.create(ReactiveUuidV7Utils.generateUuid())
                .assertNext(uuid -> assertTrue(uuids.add(uuid), "UUID should be unique"))
                .verifyComplete();
        }
        
        assertEquals(100, uuids.size());
    }

    @Test
    void generate_ShouldBeConsistentWithUuidV7Utils() {
        StepVerifier.create(ReactiveUuidV7Utils.generate())
            .assertNext(uuid -> {
                String directUuid = UuidV7Utils.generate();
                assertEquals(36, uuid.length());
                assertEquals(36, directUuid.length());
            })
            .verifyComplete();
    }
}