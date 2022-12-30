package run.ikaros.server.crd.scheme;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author: li-guohao
 */
@Slf4j
public class DefaultCRDSchemeWatcherManager implements CRDSchemeWatcherManager {

    private final List<CRDSchemeWatcher> watchers;

    public DefaultCRDSchemeWatcherManager() {
        this.watchers = new CopyOnWriteArrayList<>();
    }

    @Override
    public void register(CRDSchemeWatcher watcher) {
        Assert.notNull(watcher, "crd scheme watcher must not be null");
        if (!watchers.contains(watcher)) {
            watchers.add(watcher);
        }
    }

    @Override
    public void unregister(CRDSchemeWatcher watcher) {
        Assert.notNull(watcher, "crd scheme watcher must not be null");
        if (watchers.contains(watcher)) {
            watchers.remove(watcher);
        }
    }

    @Override
    public List<CRDSchemeWatcher> watchers() {
        return Collections.unmodifiableList(watchers);
    }
}
