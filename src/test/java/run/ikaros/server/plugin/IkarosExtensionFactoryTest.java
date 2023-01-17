package run.ikaros.server.plugin;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import run.ikaros.server.test.reflect.MemberMatcher;

class IkarosExtensionFactoryTest {

    static class CanNewInstance {
    }

    static class NotNewInstance {
        private NotNewInstance() {
        }
    }

    @Test
    void createNotUseSpring() {
        IkarosExtensionFactory ikarosExtensionFactory
            = new IkarosExtensionFactory(new IkarosPluginManager(), false);
        Assertions.assertThat(ikarosExtensionFactory.create(CanNewInstance.class))
            .isInstanceOf(CanNewInstance.class);
    }

    @Test
    void createNotUseSpringAndHasReflectException() {
        IkarosExtensionFactory ikarosExtensionFactory
            = new IkarosExtensionFactory(new IkarosPluginManager(), false);
        Assertions.assertThatExceptionOfType(PluginException.class)
            .isThrownBy(() -> ikarosExtensionFactory.create(NotNewInstance.class))
            .withMessageContaining("new extension cls instance fail");
    }

    static class TestPlugin extends Plugin {
        public TestPlugin(PluginWrapper wrapper) {
            super(wrapper);
        }
    }

    static class TestBasePlugin extends BasePlugin {

        public TestBasePlugin(PluginWrapper wrapper) {
            super(wrapper);
        }
    }


    @Test
    void createUseSpringWhenManagerNotFoundPlugin() {
        IkarosPluginManager pluginManager = Mockito.spy(new IkarosPluginManager());
        IkarosExtensionFactory extensionFactory = new IkarosExtensionFactory(pluginManager);
        PluginWrapper pluginWrapper = Mockito.mock(PluginWrapper.class);
        Mockito.when(pluginWrapper.getPlugin())
            .thenReturn(null);
        Assertions.assertThatExceptionOfType(PluginException.class)
            .isThrownBy(() -> extensionFactory.create(NotNewInstance.class))
            .withMessageContaining("new extension cls instance fail");
    }

    @Test
    void createUseSpringWhenPluginIsExtendsBasePluginAndPluginApplicationContextNoExists() {
        IkarosPluginManager pluginManager = Mockito.spy(new IkarosPluginManager());
        IkarosExtensionFactory extensionFactory = new IkarosExtensionFactory(pluginManager);
        PluginWrapper pluginWrapper = Mockito.mock(PluginWrapper.class);
        TestBasePlugin testBasePlugin = Mockito.spy(new TestBasePlugin(pluginWrapper));

        Mockito.doReturn(pluginWrapper).when(pluginManager).whichPlugin(TestBasePlugin.class);
        Mockito.doReturn(testBasePlugin).when(pluginWrapper).getPlugin();
        Mockito.doReturn("noExistsPluginId").when(pluginWrapper).getPluginId();
        Assertions.assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> extensionFactory.create(TestBasePlugin.class))
            .withMessageContaining("The plugin [%s] can not be found.", "noExistsPluginId");
    }

    @Test
    void createUseSpringWhenPluginIsExtendsBasePluginAndPluginApplicationContextExists() {
        IkarosPluginManager pluginManager = Mockito.spy(new IkarosPluginManager());
        IkarosExtensionFactory extensionFactory = new IkarosExtensionFactory(pluginManager);
        PluginWrapper pluginWrapper = Mockito.mock(PluginWrapper.class);
        TestBasePlugin testBasePlugin = Mockito.spy(new TestBasePlugin(pluginWrapper));

        Mockito.doReturn(pluginWrapper).when(pluginManager).whichPlugin(TestBasePlugin.class);
        Mockito.doReturn(testBasePlugin).when(pluginWrapper).getPlugin();

        final String pluginId = "unitTestPluginId";
        Mockito.doReturn(pluginId).when(pluginWrapper).getPluginId();
        try {
            PluginApplicationContext pluginApplicationContext =
                Mockito.mock(PluginApplicationContext.class);
            PluginApplicationContextRegistry.getInstance()
                .register(pluginId, pluginApplicationContext);
            Mockito.doReturn(testBasePlugin)
                .when(pluginApplicationContext).getBean(TestBasePlugin.class);

            Assertions.assertThat(extensionFactory.create(TestBasePlugin.class))
                .isEqualTo(testBasePlugin);
        } finally {
            PluginApplicationContextRegistry.getInstance().remove(pluginId);
        }
    }

    @Test
    void createUseSpringWhenPluginIsExtendsPluginAndPluginApplicationContextNoExists()
        throws NoSuchFieldException, IllegalAccessException {
        IkarosPluginManager pluginManager = Mockito.spy(new IkarosPluginManager());
        IkarosExtensionFactory extensionFactory = new IkarosExtensionFactory(pluginManager);
        PluginWrapper pluginWrapper = Mockito.mock(PluginWrapper.class);
        TestPlugin testPlugin = Mockito.spy(new TestPlugin(pluginWrapper));

        PluginApplicationInitializer pluginApplicationInitializer =
            Mockito.mock(PluginApplicationInitializer.class);
        MemberMatcher.field(IkarosPluginManager.class, "pluginApplicationInitializer")
            .set(pluginManager, pluginApplicationInitializer);
        Mockito.doThrow(IllegalArgumentException.class)
            .when(pluginApplicationInitializer).getPluginApplicationContext(Mockito.anyString());

        Mockito.doReturn(pluginWrapper).when(pluginManager).whichPlugin(TestPlugin.class);
        Mockito.doReturn(testPlugin).when(pluginWrapper).getPlugin();
        Mockito.doReturn("noExistsPluginId").when(pluginWrapper).getPluginId();
        Assertions.assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> extensionFactory.create(TestPlugin.class));
    }


    @Test
    void createUseSpringWhenPluginIsExtendsPluginAndPluginApplicationContextExists()
        throws NoSuchFieldException, IllegalAccessException {
        IkarosPluginManager pluginManager = Mockito.spy(new IkarosPluginManager());
        IkarosExtensionFactory extensionFactory = new IkarosExtensionFactory(pluginManager);
        PluginWrapper pluginWrapper = Mockito.mock(PluginWrapper.class);
        TestPlugin testPlugin = Mockito.spy(new TestPlugin(pluginWrapper));
        final String pluginId = "unitTestPluginId";

        PluginApplicationInitializer pluginApplicationInitializer =
            Mockito.mock(PluginApplicationInitializer.class);
        MemberMatcher.field(IkarosPluginManager.class, "pluginApplicationInitializer")
            .set(pluginManager, pluginApplicationInitializer);

        Mockito.doReturn(pluginWrapper).when(pluginManager).whichPlugin(TestPlugin.class);
        Mockito.doReturn(testPlugin).when(pluginWrapper).getPlugin();
        Mockito.doReturn(pluginId).when(pluginWrapper).getPluginId();

        try {
            PluginApplicationContext pluginApplicationContext =
                Mockito.mock(PluginApplicationContext.class);
            PluginApplicationContextRegistry.getInstance()
                .register(pluginId, pluginApplicationContext);
            Mockito.doReturn(testPlugin)
                .when(pluginApplicationContext).getBean(TestPlugin.class);
            Mockito.doReturn(pluginApplicationContext)
                .when(pluginApplicationInitializer).getPluginApplicationContext(pluginId);

            Assertions.assertThat(extensionFactory.create(TestPlugin.class))
                .isEqualTo(testPlugin);
        } finally {
            PluginApplicationContextRegistry.getInstance().remove(pluginId);
        }
    }
}