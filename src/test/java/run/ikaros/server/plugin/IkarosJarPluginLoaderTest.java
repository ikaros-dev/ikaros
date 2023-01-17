package run.ikaros.server.plugin;

import java.nio.file.Path;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.pf4j.ClassLoadingStrategy;
import org.pf4j.PluginClassLoader;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginManager;
import run.ikaros.server.test.reflect.MemberMatcher;

class IkarosJarPluginLoaderTest {

    @Test
    void loadPlugin() throws NoSuchFieldException, IllegalAccessException {
        IkarosJarPluginLoader ikarosJarPluginLoader
            = new IkarosJarPluginLoader(Mockito.mock(PluginManager.class));
        PluginClassLoader pluginClassLoader = (PluginClassLoader) ikarosJarPluginLoader
            .loadPlugin(Path.of(""),
                Mockito.mock(PluginDescriptor.class));
        ClassLoadingStrategy classLoadingStrategy =
            (ClassLoadingStrategy) MemberMatcher
                .field(PluginClassLoader.class, "classLoadingStrategy").get(pluginClassLoader);
        Assertions.assertThat(classLoadingStrategy).isEqualTo(ClassLoadingStrategy.APD);
    }
}