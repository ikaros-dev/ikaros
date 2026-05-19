package run.ikaros.server.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class PluginStartingErrorTest {

    @Test
    void of_createsInstanceWithFields() {
        PluginStartingError error = PluginStartingError.of(
            "test-plugin", "Something went wrong", "Dev details here"
        );

        assertEquals("test-plugin", error.getPluginId());
        assertEquals("Something went wrong", error.getMessage());
        assertEquals("Dev details here", error.getDevMessage());
    }

    @Test
    void equals_sameFields_returnsTrue() {
        PluginStartingError error1 = PluginStartingError.of("id", "msg", "dev");
        PluginStartingError error2 = PluginStartingError.of("id", "msg", "dev");

        assertEquals(error1, error2);
    }

    @Test
    void hashCode_sameFields_returnsSameValue() {
        PluginStartingError error1 = PluginStartingError.of("id", "msg", "dev");
        PluginStartingError error2 = PluginStartingError.of("id", "msg", "dev");

        assertEquals(error1.hashCode(), error2.hashCode());
    }

    @Test
    void toString_containsFields() {
        PluginStartingError error = PluginStartingError.of("plugin-1", "Error", "Dev");

        String str = error.toString();
        assertNotNull(str);
        assertEquals(true, str.contains("plugin-1"));
        assertEquals(true, str.contains("Error"));
        assertEquals(true, str.contains("Dev"));
    }
}
