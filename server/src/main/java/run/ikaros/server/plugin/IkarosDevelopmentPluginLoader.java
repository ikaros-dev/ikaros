package run.ikaros.server.plugin;

import java.nio.file.Path;
import org.pf4j.ClassLoadingStrategy;
import org.pf4j.DevelopmentPluginLoader;
import org.pf4j.DevelopmentPluginRepository;
import org.pf4j.PluginClassLoader;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginManager;

/**
 * An extension for {@link DevelopmentPluginRepository}
 * that override {@link DevelopmentPluginLoader#loadPlugin(Path, PluginDescriptor)}
 * to change {@link ClassLoadingStrategy}
 * from default {@link ClassLoadingStrategy#PDA} to {@link ClassLoadingStrategy#APD}.
 *
 * @author li-guohao
 * @see IkarosPluginManager
 */
public class IkarosDevelopmentPluginLoader extends DevelopmentPluginLoader {
    public IkarosDevelopmentPluginLoader(PluginManager pluginManager) {
        super(pluginManager);
    }

    @Override
    public ClassLoader loadPlugin(Path pluginPath,
                                  PluginDescriptor pluginDescriptor) {
        PluginClassLoader pluginClassLoader =
            new PluginClassLoader(pluginManager, pluginDescriptor,
                getClass().getClassLoader(), ClassLoadingStrategy.APD);

        if (pluginPath.toFile().isDirectory()) {
            loadClasses(pluginPath, pluginClassLoader);
            loadJars(pluginPath, pluginClassLoader);
        } else {
            pluginClassLoader.addFile(pluginPath.toFile());
        }

        return pluginClassLoader;
    }
}
