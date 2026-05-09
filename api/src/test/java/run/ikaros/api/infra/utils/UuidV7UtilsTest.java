package run.ikaros.api.infra.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidV7UtilsTest {

    @Test
    void generateShouldReturnValidUuidString() {
        String uuid = UuidV7Utils.generate();
        assertThat(uuid).isNotNull();
        assertThat(uuid).hasSize(36);
        // Verify it's a valid UUID format
        UUID parsed = UUID.fromString(uuid);
        assertThat(parsed).isNotNull();
    }

    @Test
    void generateShouldReturnUniqueValues() {
        String uuid1 = UuidV7Utils.generate();
        String uuid2 = UuidV7Utils.generate();
        assertThat(uuid1).isNotEqualTo(uuid2);
    }

    @Test
    void generateUuidShouldReturnNonNull() {
        UUID uuid = UuidV7Utils.generateUuid();
        assertThat(uuid).isNotNull();
    }

    @Test
    void generateUuidShouldReturnUniqueValues() {
        UUID uuid1 = UuidV7Utils.generateUuid();
        UUID uuid2 = UuidV7Utils.generateUuid();
        assertThat(uuid1).isNotEqualTo(uuid2);
    }

    @Test
    void generateStringShouldMatchGenerateUuid() {
        // Both methods should produce valid UUIDs
        String strUuid = UuidV7Utils.generate();
        UUID objUuid = UuidV7Utils.generateUuid();

        // Both should be valid
        UUID.fromString(strUuid);
        assertThat(objUuid.toString()).hasSize(36);
    }
}
