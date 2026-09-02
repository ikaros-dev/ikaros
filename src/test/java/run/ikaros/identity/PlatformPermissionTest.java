package run.ikaros.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class PlatformPermissionTest {
    @Test
    void registryUsesStableKeys() {
        assertEquals("resource.read", PlatformPermission.fromKey("resource.read").key());
        assertEquals(PlatformPermission.values().length, PlatformPermission.registeredKeys().size());
    }

    @Test
    void unknownPermissionCannotBeUsed() {
        assertThrows(IllegalArgumentException.class, () -> PlatformPermission.fromKey("resource.unknown"));
    }
}
