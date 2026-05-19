package run.ikaros.server.plugin.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class IkarosPluginDeleteEventTest {

    @Test
    void constructorAndGetters() {
        Object source = new Object();
        String pluginId = "test-plugin-001";
        IkarosPluginDeleteEvent event = new IkarosPluginDeleteEvent(source, pluginId);

        assertSame(source, event.getSource());
        assertEquals(pluginId, event.getPluginId());
    }

    @Test
    void isApplicationEvent() {
        IkarosPluginDeleteEvent event =
            new IkarosPluginDeleteEvent(new Object(), "some-plugin");
        assertNotNull(event.getTimestamp());
    }

    @Test
    void constructorWithNullPluginId() {
        Object source = new Object();
        IkarosPluginDeleteEvent event = new IkarosPluginDeleteEvent(source, null);

        assertSame(source, event.getSource());
        assertEquals(null, event.getPluginId());
    }

    @Test
    void constructorWithEmptyPluginId() {
        Object source = new Object();
        IkarosPluginDeleteEvent event = new IkarosPluginDeleteEvent(source, "");

        assertSame(source, event.getSource());
        assertEquals("", event.getPluginId());
    }

    @Test
    void differentPluginIdsAreDistinct() {
        Object source = new Object();
        IkarosPluginDeleteEvent event1 =
            new IkarosPluginDeleteEvent(source, "plugin-a");
        IkarosPluginDeleteEvent event2 =
            new IkarosPluginDeleteEvent(source, "plugin-b");

        assertEquals("plugin-a", event1.getPluginId());
        assertEquals("plugin-b", event2.getPluginId());
        assertNotEquals(event1.getPluginId(), event2.getPluginId());
    }

    private static void assertNotEquals(String a, String b) {
        org.junit.jupiter.api.Assertions.assertNotEquals(a, b);
    }
}
