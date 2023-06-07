package run.ikaros.server.plugin;

import java.io.File;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.pf4j.ClassLoadingStrategy;
import org.pf4j.PluginClassLoader;
import org.pf4j.PluginDescriptor;
import run.ikaros.server.test.reflect.MemberMatcher;

class IkarosJarPluginLoaderTest {

    @Test
    void loadPlugin() throws NoSuchFieldException, IllegalAccessException {
        IkarosJarPluginLoader ikarosJarPluginLoader
            = new IkarosJarPluginLoader(new IkarosPluginManager());
        PluginClassLoader pluginClassLoader = (PluginClassLoader) ikarosJarPluginLoader
            .loadPlugin(new File("").toPath(),
                Mockito.mock(PluginDescriptor.class));
        ClassLoadingStrategy classLoadingStrategy =
            (ClassLoadingStrategy) MemberMatcher
                .field(PluginClassLoader.class, "classLoadingStrategy").get(pluginClassLoader);
        Assertions.assertThat(classLoadingStrategy).isEqualTo(ClassLoadingStrategy.APD);
    }
}