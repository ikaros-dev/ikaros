package run.ikaros.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InMemoryPluginRuntimeTest {
    private final PluginManifest manifest = new PluginManifest("example.plugin", "Example", "1.0.0",
        "Example", "1", "2.0.0", null, "example.Entry", List.of("parser"),
        List.of("resource.read"), List.of("parser"));

    @Test
    void lifecycleRequiresDeclaredPermissionsAndExplicitEnablement() {
        InMemoryPluginRuntime runtime = new InMemoryPluginRuntime("2.0.0");
        PluginDescriptor installed = runtime.install(manifest, Set.of("resource.read")).block();
        assertEquals(PluginLifecycle.INSTALLED, installed.lifecycle());
        assertEquals(PluginLifecycle.ENABLED, runtime.enable(manifest.pluginId()).block().lifecycle());
        assertEquals(PluginLifecycle.DISABLED, runtime.disable(manifest.pluginId()).block().lifecycle());
    }

    @Test
    void rejectsUndeclaredPermission() {
        InMemoryPluginRuntime runtime = new InMemoryPluginRuntime("2.0.0");
        assertThrows(RuntimeException.class,
            () -> runtime.install(manifest, Set.of("resource.write")).block());
    }

    @Test
    void rejectsUnsupportedPluginApiVersion() {
        InMemoryPluginRuntime runtime = new InMemoryPluginRuntime("2.0.0", "2", null);
        assertThrows(RuntimeException.class, () -> runtime.install(manifest, Set.of()).block());
    }

    @Test
    void comparesServerVersionsNumerically() {
        InMemoryPluginRuntime runtime = new InMemoryPluginRuntime("10.0.0");
        PluginManifest minimumTen = new PluginManifest("versioned.plugin", "Versioned", "1.0.0",
            "Example", "1", "2.0.0", null, "example.Entry", List.of(), List.of(), List.of());
        assertEquals(PluginLifecycle.INSTALLED, runtime.install(minimumTen, Set.of()).block().lifecycle());
    }
}
