package run.ikaros.server.custom.scheme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import run.ikaros.server.custom.scheme.CustomSchemeWatcherManager.SchemeWatcher;

class DefaultCustomSchemeWatcherManagerTest {

    private DefaultCustomSchemeWatcherManager watcherManager;

    @BeforeEach
    void setUp() {
        watcherManager = new DefaultCustomSchemeWatcherManager();
    }

    @Test
    void register() {
        SchemeWatcher watcher = event -> {};
        watcherManager.register(watcher);
        assertThat(watcherManager.watchers()).hasSize(1).contains(watcher);
    }

    @Test
    @SuppressWarnings("NullAway")
    void registerNullThrows() {
        assertThatThrownBy(() -> watcherManager.register(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unregister() {
        SchemeWatcher watcher = event -> {};
        watcherManager.register(watcher);
        watcherManager.unregister(watcher);
        assertThat(watcherManager.watchers()).isEmpty();
    }

    @Test
    @SuppressWarnings("NullAway")
    void unregisterNullThrows() {
        assertThatThrownBy(() -> watcherManager.unregister(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unregisterNonExistent() {
        SchemeWatcher watcher = event -> {};
        watcherManager.unregister(watcher);
        assertThat(watcherManager.watchers()).isEmpty();
    }

    @Test
    void watchersReturnsUnmodifiableList() {
        SchemeWatcher watcher = event -> {};
        watcherManager.register(watcher);
        assertThat(watcherManager.watchers()).isUnmodifiable();
    }

    @Test
    void multipleWatchers() {
        SchemeWatcher w1 = event -> {};
        SchemeWatcher w2 = event -> {};
        watcherManager.register(w1);
        watcherManager.register(w2);
        assertThat(watcherManager.watchers()).hasSize(2).containsExactly(w1, w2);
    }
}
