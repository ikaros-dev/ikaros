package run.ikaros.server.plugin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.pf4j.DevelopmentPluginRepository;
import org.springframework.util.CollectionUtils;

/**
 * An extension for {@link DevelopmentPluginRepository}
 * that override {@link DevelopmentPluginRepository#getPluginPaths()}
 * to support fixed paths.
 */
public class FixedPathDevelopmentPluginRepository extends DevelopmentPluginRepository {

    private final List<Path> fixedPaths = new ArrayList<>();

    public FixedPathDevelopmentPluginRepository(Path... pluginsRoots) {
        super(pluginsRoots);
    }

    public FixedPathDevelopmentPluginRepository(List<Path> pluginsRoots) {
        super(pluginsRoots);
    }


    public void addFixedPath(Path path) {
        fixedPaths.add(path);
    }

    /**
     * Set develop plugin repository fixed path list.
     *
     * @param paths plugin fixed path list
     */
    public void setFixedPaths(List<Path> paths) {
        if (CollectionUtils.isEmpty(paths)) {
            return;
        }
        fixedPaths.clear();
        fixedPaths.addAll(paths);
    }

    @Override
    public List<Path> getPluginPaths() {
        List<Path> paths = new ArrayList<>(fixedPaths);
        paths.addAll(super.getPluginPaths());
        return paths;
    }
}
