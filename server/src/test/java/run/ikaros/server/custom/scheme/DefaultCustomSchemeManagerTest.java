package run.ikaros.server.custom.scheme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import run.ikaros.api.custom.GroupVersionKind;
import run.ikaros.api.custom.scheme.CustomScheme;
import run.ikaros.server.custom.scheme.CustomSchemeWatcherManager.SchemeWatcher;

@ExtendWith(MockitoExtension.class)
class DefaultCustomSchemeManagerTest {

    @Mock
    private CustomSchemeWatcherManager watcherManager;
    @Mock
    private SchemeWatcher schemeWatcher;

    private DefaultCustomSchemeManager manager;
    private CustomScheme scheme;
    private ObjectNode schema;

    @BeforeEach
    @SuppressWarnings("NullAway.Init")
    void setUp() throws Exception {
        manager = new DefaultCustomSchemeManager(watcherManager);
        ObjectMapper mapper = new ObjectMapper();
        schema = mapper.createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties");
        scheme = new CustomScheme(
            Object.class,
            new GroupVersionKind("test.io", "v1", "Test"),
            "tests",
            "test",
            schema
        );
    }

    @Test
    void register() {
        manager.register(scheme);
        assertThat(manager.schemes())
            .hasSize(1)
            .contains(scheme);
    }

    @Test
    void registerNotifiesWatcher() {
        when(watcherManager.watchers()).thenReturn(List.of(schemeWatcher));
        manager.register(scheme);
        verify(schemeWatcher).onChange(any(CustomSchemeWatcherManager.SchemeRegistered.class));
    }

    @Test
    void registerDuplicate() {
        manager.register(scheme);
        manager.register(scheme);
        assertThat(manager.schemes()).hasSize(1);
    }

    @Test
    void unregister() {
        manager.register(scheme);
        manager.unregister(scheme);
        assertThat(manager.schemes()).isEmpty();
    }

    @Test
    void unregisterNotifiesWatcher() {
        when(watcherManager.watchers()).thenReturn(List.of(schemeWatcher));
        manager.register(scheme);
        manager.unregister(scheme);
        verify(schemeWatcher).onChange(any(CustomSchemeWatcherManager.SchemeUnregistered.class));
    }

    @Test
    void unregisterNonExistent() {
        manager.unregister(scheme);
        assertThat(manager.schemes()).isEmpty();
    }

    @Test
    void registerWithNullWatcherManager() {
        DefaultCustomSchemeManager noWatcherManager = new DefaultCustomSchemeManager(null);
        noWatcherManager.register(scheme);
        assertThat(noWatcherManager.schemes()).hasSize(1);
    }

    @Test
    void schemesReturnsUnmodifiableList() {
        manager.register(scheme);
        assertThat(manager.schemes()).isUnmodifiable();
    }

    @Test
    void unregisterNoWatchers() {
        when(watcherManager.watchers()).thenReturn(null);
        manager.register(scheme);
        manager.unregister(scheme);
        assertThat(manager.schemes()).isEmpty();
    }
}
