package run.ikaros.server.core.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.server.plugin.IkarosPluginManager;
import run.ikaros.server.plugin.PluginValidationException;

@org.jspecify.annotations.NullUnmarked
class PluginServiceImplTest {

    /** 测试临时目录. */
    @TempDir
    Path tempDir;

    private IkarosPluginManager pluginManager;
    private ReactiveCustomClient customClient;
    private PluginServiceImpl pluginService;

    @BeforeEach
    void setUp() {
        pluginManager = mock(IkarosPluginManager.class);
        customClient = mock(ReactiveCustomClient.class);
        pluginService = new PluginServiceImpl(pluginManager, customClient);
    }

    @Test
    void constructorSetsFields() {
        PluginServiceImpl service = new PluginServiceImpl(pluginManager, customClient);
        assertThat(service).isNotNull();
    }

    @Test
    void operateStateStartDelegatesToPluginManager() {
        String pluginId = "test-plugin";
        PluginWrapper pluginWrapper = mock(PluginWrapper.class);
        when(pluginManager.getPlugin(pluginId)).thenReturn(pluginWrapper);
        when(pluginWrapper.getPluginState()).thenReturn(PluginState.STARTED);
        when(pluginManager.getPlugins()).thenReturn(List.of(pluginWrapper));

        StepVerifier.create(pluginService.operateState(pluginId, PluginStateOperate.START))
            .assertNext(state -> assertThat(state).isEqualTo(PluginState.STARTED))
            .verifyComplete();

        verify(pluginManager).startPlugin(pluginId);
    }

    @Test
    void operateStateStopDelegatesToPluginManager() {
        String pluginId = "test-plugin";
        PluginWrapper pluginWrapper = mock(PluginWrapper.class);
        when(pluginManager.getPlugin(pluginId)).thenReturn(pluginWrapper);
        when(pluginWrapper.getPluginState()).thenReturn(PluginState.STOPPED);
        when(pluginManager.getPlugins()).thenReturn(List.of(pluginWrapper));

        StepVerifier.create(pluginService.operateState(pluginId, PluginStateOperate.STOP))
            .assertNext(state -> assertThat(state).isEqualTo(PluginState.STOPPED))
            .verifyComplete();

        verify(pluginManager).stopPlugin(pluginId);
    }

    @Test
    void operateStateReloadDelegatesToPluginManager() {
        String pluginId = "test-plugin";
        PluginWrapper pluginWrapper = mock(PluginWrapper.class);
        when(pluginManager.getPlugin(pluginId)).thenReturn(pluginWrapper);
        when(pluginWrapper.getPluginState()).thenReturn(PluginState.STARTED);
        when(pluginManager.getPlugins()).thenReturn(List.of(pluginWrapper));

        StepVerifier.create(pluginService.operateState(pluginId, PluginStateOperate.RELOAD))
            .assertNext(state -> assertThat(state).isEqualTo(PluginState.STARTED))
            .verifyComplete();

        verify(pluginManager).reloadPlugin(pluginId);
    }

    @Test
    void operateStateLoadDelegatesToPluginManager() {
        String pluginId = "test-plugin";
        PluginWrapper pluginWrapper = mock(PluginWrapper.class);
        when(pluginManager.getPlugin(pluginId)).thenReturn(pluginWrapper);
        when(pluginWrapper.getPluginState()).thenReturn(PluginState.RESOLVED);
        when(pluginManager.getPlugins()).thenReturn(List.of(pluginWrapper));

        StepVerifier.create(pluginService.operateState(pluginId, PluginStateOperate.LOAD))
            .assertNext(state -> assertThat(state).isEqualTo(PluginState.RESOLVED))
            .verifyComplete();

        verify(pluginManager).loadPlugin(pluginId);
    }

    @Test
    void operateStateUnloadDelegatesToPluginManager() {
        String pluginId = "test-plugin";
        when(pluginManager.getPlugin(pluginId)).thenReturn(null);
        when(pluginManager.getPlugins()).thenReturn(List.of());

        StepVerifier.create(pluginService.operateState(pluginId, PluginStateOperate.UNLOAD))
            .verifyComplete();

        verify(pluginManager).unloadPlugin(pluginId);
    }

    @Test
    void operateStateEnableDelegatesToPluginManager() {
        String pluginId = "test-plugin";
        PluginWrapper pluginWrapper = mock(PluginWrapper.class);
        when(pluginManager.getPlugin(pluginId)).thenReturn(pluginWrapper);
        when(pluginWrapper.getPluginState()).thenReturn(PluginState.RESOLVED);
        when(pluginManager.getPlugins()).thenReturn(List.of(pluginWrapper));

        StepVerifier.create(pluginService.operateState(pluginId, PluginStateOperate.ENABLE))
            .assertNext(state -> assertThat(state).isEqualTo(PluginState.RESOLVED))
            .verifyComplete();

        verify(pluginManager).enablePlugin(pluginId);
    }

    @Test
    void operateStateDisableDelegatesToPluginManager() {
        String pluginId = "test-plugin";
        PluginWrapper pluginWrapper = mock(PluginWrapper.class);
        when(pluginManager.getPlugin(pluginId)).thenReturn(pluginWrapper);
        when(pluginWrapper.getPluginState()).thenReturn(PluginState.DISABLED);
        when(pluginManager.getPlugins()).thenReturn(List.of(pluginWrapper));

        StepVerifier.create(pluginService.operateState(pluginId, PluginStateOperate.DISABLE))
            .assertNext(state -> assertThat(state).isEqualTo(PluginState.DISABLED))
            .verifyComplete();

        verify(pluginManager).disablePlugin(pluginId);
    }

    @Test
    void operateStateDeleteDelegatesToPluginManager() {
        String pluginId = "test-plugin";
        when(pluginManager.getPlugins()).thenReturn(List.of());

        StepVerifier.create(pluginService.operateState(pluginId, PluginStateOperate.DELETE))
            .verifyComplete();

        verify(pluginManager).deletePlugin(pluginId);
    }

    @Test
    void operateStateWithBlankPluginIdThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> pluginService.operateState("", PluginStateOperate.START));
    }

    @Test
    void operateStateWithNullOperateThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> pluginService.operateState("test-plugin", null));
    }

    @Test
    void startReturnsPluginState() {
        String pluginId = "test-plugin";
        PluginWrapper pluginWrapper = mock(PluginWrapper.class);
        when(pluginManager.getPlugin(pluginId)).thenReturn(pluginWrapper);
        when(pluginManager.startPlugin(pluginId)).thenReturn(PluginState.STARTED);

        StepVerifier.create(pluginService.start(pluginId))
            .assertNext(state -> assertThat(state).isEqualTo(PluginState.STARTED))
            .verifyComplete();
    }

    @Test
    void startReturnsErrorWhenPluginNotFound() {
        String pluginId = "nonexistent-plugin";
        when(pluginManager.getPlugin(pluginId)).thenReturn(null);

        StepVerifier.create(pluginService.start(pluginId))
            .expectError(NotFoundException.class)
            .verify();
    }

    @Test
    void startReturnsErrorWhenPluginIdIsBlank() {
        assertThrows(IllegalArgumentException.class,
            () -> pluginService.start(""));
    }

    @Test
    void stopReturnsPluginState() {
        String pluginId = "test-plugin";
        PluginWrapper pluginWrapper = mock(PluginWrapper.class);
        when(pluginManager.getPlugin(pluginId)).thenReturn(pluginWrapper);
        when(pluginManager.stopPlugin(pluginId)).thenReturn(PluginState.STOPPED);

        StepVerifier.create(pluginService.stop(pluginId))
            .assertNext(state -> assertThat(state).isEqualTo(PluginState.STOPPED))
            .verifyComplete();
    }

    @Test
    void stopReturnsErrorWhenPluginNotFound() {
        String pluginId = "nonexistent-plugin";
        when(pluginManager.getPlugin(pluginId)).thenReturn(null);

        StepVerifier.create(pluginService.stop(pluginId))
            .expectError(NotFoundException.class)
            .verify();
    }

    @Test
    void stopReturnsErrorWhenPluginIdIsBlank() {
        assertThrows(IllegalArgumentException.class,
            () -> pluginService.stop(""));
    }

    @Test
    void reloadReturnsPluginState() {
        String pluginId = "test-plugin";
        PluginWrapper pluginWrapper = mock(PluginWrapper.class);
        when(pluginManager.getPlugin(pluginId)).thenReturn(pluginWrapper);
        when(pluginManager.reloadPlugin(pluginId)).thenReturn(PluginState.STARTED);

        StepVerifier.create(pluginService.reload(pluginId))
            .assertNext(state -> assertThat(state).isEqualTo(PluginState.STARTED))
            .verifyComplete();
    }

    @Test
    void reloadReturnsErrorWhenPluginNotFound() {
        String pluginId = "nonexistent-plugin";
        when(pluginManager.getPlugin(pluginId)).thenReturn(null);

        StepVerifier.create(pluginService.reload(pluginId))
            .expectError(NotFoundException.class)
            .verify();
    }

    @Test
    void reloadReturnsErrorWhenPluginIdIsBlank() {
        assertThrows(IllegalArgumentException.class,
            () -> pluginService.reload(""));
    }

    @Test
    void operateStateLoadAllDelegatesToPluginManager() {
        when(pluginManager.getPlugins()).thenReturn(List.of());

        StepVerifier.create(pluginService.operateState("ALL", PluginStateOperate.LOAD_ALL))
            .verifyComplete();

        verify(pluginManager).loadPlugins();
    }

    @Test
    void operateStateReloadAllDelegatesToPluginManager() {
        when(pluginManager.getPlugins()).thenReturn(List.of());

        StepVerifier.create(pluginService.operateState("ALL", PluginStateOperate.RELOAD_ALL))
            .verifyComplete();

        verify(pluginManager).reloadPlugins();
    }

    @Test
    void operateStateReloadAllStartedDelegatesToPluginManager() {
        when(pluginManager.getPlugins()).thenReturn(List.of());

        StepVerifier.create(
                pluginService.operateState("ALL", PluginStateOperate.RELOAD_ALL_STARTED))
            .verifyComplete();

        verify(pluginManager).reloadStartedPlugins();
    }

    @Test
    void upgradeRestoresOldPluginWhenNewPluginValidationFails() throws IOException {
        String pluginId = "test-plugin";
        Path oldPath = tempDir.resolve("test-plugin.jar");
        Files.writeString(oldPath, "old-plugin", StandardCharsets.UTF_8);
        PluginWrapper oldPlugin = mock(PluginWrapper.class);
        FilePart filePart = mock(FilePart.class);
        when(pluginManager.getPlugin(pluginId)).thenReturn(oldPlugin).thenReturn(null);
        when(oldPlugin.getPluginPath()).thenReturn(oldPath);
        when(oldPlugin.getPluginState()).thenReturn(PluginState.STARTED);
        when(pluginManager.unloadPlugin(pluginId)).thenReturn(true);
        when(pluginManager.loadPlugin(oldPath)).thenReturn(pluginId);
        when(filePart.filename()).thenReturn("test-plugin.jar");
        when(filePart.transferTo(any(File.class)))
            .thenReturn(Mono.error(new PluginValidationException("invalid plugin")));

        String originalPluginsDir = System.getProperty("pf4j.pluginsDir");
        System.setProperty("pf4j.pluginsDir", tempDir.toString());
        try {
            StepVerifier.create(pluginService.upgrade(pluginId, filePart))
                .expectError(PluginValidationException.class)
                .verify();
        } finally {
            if (originalPluginsDir == null) {
                System.clearProperty("pf4j.pluginsDir");
            } else {
                System.setProperty("pf4j.pluginsDir", originalPluginsDir);
            }
        }

        assertThat(Files.readString(oldPath, StandardCharsets.UTF_8)).isEqualTo("old-plugin");
        try (var paths = Files.list(tempDir)) {
            assertThat(paths.map(Path::getFileName).map(Path::toString).toList())
                .containsExactly("test-plugin.jar");
        }
        verify(pluginManager).validatePlugin(pluginId);
        verify(pluginManager).startPlugin(pluginId);
    }
}
