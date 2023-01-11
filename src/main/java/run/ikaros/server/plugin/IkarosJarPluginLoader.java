package run.ikaros.server.plugin;

import java.nio.file.Path;
import org.pf4j.ClassLoadingStrategy;
import org.pf4j.JarPluginLoader;
import org.pf4j.PluginClassLoader;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginManager;

/**
 * An extension for {@link JarPluginLoader}
 * that override {@link JarPluginLoader#loadPlugin(Path, PluginDescriptor)}
 * to change {@link ClassLoadingStrategy}
 * from default {@link ClassLoadingStrategy#PDA} to {@link ClassLoadingStrategy#APD}.
 *
 * @see IkarosPluginManager
 * @author li-guohao
 */
public class IkarosJarPluginLoader extends JarPluginLoader {
    public IkarosJarPluginLoader(PluginManager pluginManager) {
        super(pluginManager);
    }

    @Override
    public ClassLoader loadPlugin(Path pluginPath,
                                  PluginDescriptor pluginDescriptor) {
        PluginClassLoader pluginClassLoader =
            new PluginClassLoader(pluginManager, pluginDescriptor,
                getClass().getClassLoader(), ClassLoadingStrategy.APD);
        pluginClassLoader.addFile(pluginPath.toFile());
        return pluginClassLoader;

    }
}
