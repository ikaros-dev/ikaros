package run.ikaros.server.crd.scheme;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author: li-guohao
 */
public class DefaultCRDSchemeManager implements CRDSchemeManager {

    private final List<CRDScheme> schemes;

    @Nullable
    private CRDSchemeWatcherManager watcherManager;

    public DefaultCRDSchemeManager(@Nullable CRDSchemeWatcherManager schemeWatcherManager) {
        this.watcherManager = schemeWatcherManager;
        this.schemes = new CopyOnWriteArrayList<>();
    }

    @NonNull
    private List<CRDSchemeWatcherManager.CRDSchemeWatcher> getWatchers() {
        if (this.watcherManager == null) {
            return Collections.emptyList();
        }
        return Optional.ofNullable(this.watcherManager.watchers()).orElse(Collections.emptyList());
    }

    @Override
    public void register(@NonNull CRDScheme scheme) {
        Assert.notNull(scheme, "crd scheme must not be null");
        if (!schemes.contains(scheme)) {
            schemes.add(scheme);
            getWatchers().forEach(schemeWatcher -> schemeWatcher.onChange(
                new CRDSchemeWatcherManager.RegisteredCRDSchemeEvent(scheme)));
        }
    }

    @Override
    public void unregister(@Nonnull CRDScheme scheme) {
        Assert.notNull(scheme, "crd scheme must not be null");
        if (schemes.contains(scheme)) {
            schemes.remove(scheme);
            getWatchers().forEach(schemeWatcher -> schemeWatcher.onChange(
                new CRDSchemeWatcherManager.UnregisteredCRDSchemeEvent(scheme)
            ));
        }
    }

    @Nonnull
    @Override
    public List<CRDScheme> schemes() {
        return Collections.unmodifiableList(schemes);
    }
}
