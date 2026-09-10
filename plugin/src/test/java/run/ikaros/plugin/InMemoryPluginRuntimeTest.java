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

    @Test
    void disablingPluginRevokesRegisteredExtensions() {
        InMemoryPluginExtensionRegistry registry = new InMemoryPluginExtensionRegistry();
        InMemoryPluginRuntime runtime = new InMemoryPluginRuntime("2.0.0", registry);
        runtime.install(manifest, Set.of("resource.read")).block();
        runtime.enable(manifest.pluginId()).block();
        assertEquals(1, registry.find("parser").size());

        runtime.disable(manifest.pluginId()).block();

        assertEquals(0, registry.find("parser").size());
        assertEquals(PluginLifecycle.DISABLED, runtime.get(manifest.pluginId()).block().lifecycle());
    }

    @Test
    void cannotDisablePluginBeforeItIsEnabled() {
        InMemoryPluginRuntime runtime = new InMemoryPluginRuntime("2.0.0");
        runtime.install(manifest, Set.of("resource.read")).block();

        assertThrows(RuntimeException.class, () -> runtime.disable(manifest.pluginId()).block());
    }

    @Test
    void upgradeKeepsLifecycleAndReplacesManifest() {
        InMemoryPluginRuntime runtime = new InMemoryPluginRuntime("2.0.0");
        runtime.install(manifest, Set.of("resource.read")).block();
        PluginManifest upgraded = new PluginManifest("example.plugin", "Example 2", "2.0.0",
            "Example", "1", "2.0.0", null, "example.EntryV2", List.of("parser"),
            List.of("resource.read"), List.of("parser"));

        PluginDescriptor result = runtime.upgrade(manifest.pluginId(), upgraded, Set.of("resource.read")).block();

        assertEquals(PluginLifecycle.INSTALLED, result.lifecycle());
        assertEquals("2.0.0", result.manifest().version());
    }

    @Test
    void incompatibleUpgradeLeavesExistingPluginUntouched() {
        InMemoryPluginRuntime runtime = new InMemoryPluginRuntime("2.0.0");
        runtime.install(manifest, Set.of("resource.read")).block();
        PluginManifest incompatible = new PluginManifest("example.plugin", "Example 2", "2.0.0",
            "Example", "1", "3.0.0", null, "example.EntryV2", List.of(), List.of(), List.of());

        assertThrows(RuntimeException.class, () -> runtime.upgrade(manifest.pluginId(), incompatible, Set.of()).block());
        assertEquals("1.0.0", runtime.get(manifest.pluginId()).block().manifest().version());
    }

    @Test
    void uninstallCanKeepPluginDataAsUninstalledRecord() {
        InMemoryPluginRuntime runtime = new InMemoryPluginRuntime("2.0.0");
        runtime.install(manifest, Set.of("resource.read")).block();

        runtime.uninstall(manifest.pluginId(), PluginUninstallPolicy.KEEP_DATA).block();

        assertEquals(PluginLifecycle.UNINSTALLED, runtime.get(manifest.pluginId()).block().lifecycle());
    }

    @Test
    void enabledPluginCannotBeUninstalledWithEitherPolicy() {
        InMemoryPluginRuntime runtime = new InMemoryPluginRuntime("2.0.0");
        runtime.install(manifest, Set.of("resource.read")).block();
        runtime.enable(manifest.pluginId()).block();

        assertThrows(RuntimeException.class,
            () -> runtime.uninstall(manifest.pluginId(), PluginUninstallPolicy.KEEP_DATA).block());
    }
}
