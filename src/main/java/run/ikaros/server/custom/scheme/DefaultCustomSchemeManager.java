package run.ikaros.server.custom.scheme;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class DefaultCustomSchemeManager implements CustomSchemeManager {

    private final List<CustomScheme> schemes;

    @Nullable
    private final CustomSchemeWatcherManager watcherManager;

    /**
     * Construct a {@link DefaultCustomSchemeManager} by {@link CustomSchemeWatcherManager}.
     */
    public DefaultCustomSchemeManager(@Nullable CustomSchemeWatcherManager watcherManager) {
        this.watcherManager = watcherManager;
        // we have to use CopyOnWriteArrayList at here to prevent concurrent modification between
        // registering and listing.
        schemes = new CopyOnWriteArrayList<>();
    }

    @Override
    public void register(@NonNull CustomScheme scheme) {
        if (!schemes.contains(scheme)) {
            schemes.add(scheme);
            getWatchers().forEach(watcher -> watcher.onChange(
                new CustomSchemeWatcherManager.SchemeRegistered(scheme)));
        }
    }

    @Override
    public void unregister(@NonNull CustomScheme scheme) {
        if (schemes.contains(scheme)) {
            schemes.remove(scheme);
            getWatchers().forEach(watcher -> watcher.onChange(
                new CustomSchemeWatcherManager.SchemeUnregistered(scheme)));
        }
    }

    @Override
    @NonNull
    public List<CustomScheme> schemes() {
        return Collections.unmodifiableList(schemes);
    }

    @NonNull
    private List<CustomSchemeWatcherManager.SchemeWatcher> getWatchers() {
        if (this.watcherManager == null) {
            return Collections.emptyList();
        }
        return Optional.ofNullable(this.watcherManager.watchers()).orElse(Collections.emptyList());
    }
}
