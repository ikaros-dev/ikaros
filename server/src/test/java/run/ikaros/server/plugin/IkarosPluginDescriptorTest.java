package run.ikaros.server.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import run.ikaros.api.plugin.custom.Plugin;

class IkarosPluginDescriptorTest {

    @Test
    void constructorSetsBasicFields() {
        IkarosPluginDescriptor descriptor = new IkarosPluginDescriptor(
            "test-plugin", "A test plugin", "com.example.TestPlugin",
            "1.0.0", "ikaros>=1.0.0", "Test Provider", "MIT"
        );

        assertEquals("test-plugin", descriptor.getPluginId());
        assertEquals("A test plugin", descriptor.getPluginDescription());
        assertEquals("com.example.TestPlugin", descriptor.getPluginClass());
        assertEquals("1.0.0", descriptor.getVersion());
        assertEquals("ikaros>=1.0.0", descriptor.getRequires());
        assertEquals("Test Provider", descriptor.getProvider());
        assertEquals("MIT", descriptor.getLicense());
    }

    @Test
    void customFieldsDefaultToNull() {
        IkarosPluginDescriptor descriptor = new IkarosPluginDescriptor(
            "id", "desc", "class", "1.0", null, null, null
        );

        assertNull(descriptor.getAuthor());
        assertNull(descriptor.getLogo());
        assertNull(descriptor.getHomepage());
        assertNull(descriptor.getDisplayName());
        assertNull(descriptor.getLoadLocation());
        assertNull(descriptor.getConfigMapSchemas());
    }

    @Test
    void settersAndGetters() {
        IkarosPluginDescriptor descriptor = new IkarosPluginDescriptor(
            "id", "desc", "class", "1.0", null, null, null
        );

        Plugin.Author author = new Plugin.Author();
        author.setName("Test Author");
        Path loadLocation = Paths.get("/tmp/plugin.jar");

        descriptor.setAuthor(author);
        descriptor.setLogo("logo.png");
        descriptor.setHomepage("https://example.com");
        descriptor.setDisplayName("Test Display");
        descriptor.setLoadLocation(loadLocation);
        descriptor.setConfigMapSchemas("{\"key\":\"value\"}");

        assertEquals(author, descriptor.getAuthor());
        assertEquals("logo.png", descriptor.getLogo());
        assertEquals("https://example.com", descriptor.getHomepage());
        assertEquals("Test Display", descriptor.getDisplayName());
        assertEquals(loadLocation, descriptor.getLoadLocation());
        assertEquals("{\"key\":\"value\"}", descriptor.getConfigMapSchemas());
    }
}
