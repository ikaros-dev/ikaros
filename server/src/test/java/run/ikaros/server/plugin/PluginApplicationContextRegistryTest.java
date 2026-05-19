package run.ikaros.server.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PluginApplicationContextRegistryTest {

    @Test
    void getInstance_returnsSameInstance() {
        PluginApplicationContextRegistry instance1 = PluginApplicationContextRegistry.getInstance();
        PluginApplicationContextRegistry instance2 = PluginApplicationContextRegistry.getInstance();
        assertSame(instance1, instance2);
    }

    @Test
    void register_andGetByPluginId() {
        PluginApplicationContextRegistry registry = PluginApplicationContextRegistry.getInstance();
        PluginApplicationContext context = new PluginApplicationContext("test-plugin-registry");

        try {
            registry.register("test-plugin-registry", context);
            PluginApplicationContext retrieved = registry.getByPluginId("test-plugin-registry");
            assertSame(context, retrieved);
        } finally {
            registry.remove("test-plugin-registry");
        }
    }

    @Test
    void getByPluginId_notFound_throwsException() {
        PluginApplicationContextRegistry registry = PluginApplicationContextRegistry.getInstance();
        assertThrows(IllegalArgumentException.class,
            () -> registry.getByPluginId("nonexistent-plugin-xyz"));
    }

    @Test
    void remove_returnsContext() {
        PluginApplicationContextRegistry registry = PluginApplicationContextRegistry.getInstance();
        PluginApplicationContext context = new PluginApplicationContext("test-plugin-remove");

        registry.register("test-plugin-remove", context);
        PluginApplicationContext removed = registry.remove("test-plugin-remove");

        assertSame(context, removed);
        assertFalse(registry.containsContext("test-plugin-remove"));
    }

    @Test
    void remove_nonexistent_returnsNull() {
        PluginApplicationContextRegistry registry = PluginApplicationContextRegistry.getInstance();
        PluginApplicationContext removed = registry.remove("nonexistent-remove-xyz");
        assertEquals(null, removed);
    }

    @Test
    void containsContext_returnsCorrectly() {
        PluginApplicationContextRegistry registry = PluginApplicationContextRegistry.getInstance();
        PluginApplicationContext context = new PluginApplicationContext("test-plugin-contains");

        try {
            assertFalse(registry.containsContext("test-plugin-contains"));
            registry.register("test-plugin-contains", context);
            assertTrue(registry.containsContext("test-plugin-contains"));
        } finally {
            registry.remove("test-plugin-contains");
        }
    }

    @Test
    void getPluginApplicationContexts_returnsList() {
        PluginApplicationContextRegistry registry = PluginApplicationContextRegistry.getInstance();
        PluginApplicationContext context = new PluginApplicationContext("test-plugin-list");

        try {
            registry.register("test-plugin-list", context);
            var contexts = registry.getPluginApplicationContexts();
            assertNotNull(contexts);
            assertTrue(contexts.contains(context));
        } finally {
            registry.remove("test-plugin-list");
        }
    }
}
