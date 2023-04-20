package run.ikaros.server.plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.pf4j.ExtensionPoint;
import org.springframework.context.ApplicationContext;

class ExtensionComponentsFinderTest {

    static class NotExtensionPoint {
    }

    static class UnitTestExtensionPoint implements ExtensionPoint {
    }

    @Test
    void getExtensionsWhenTypeIsNotImplementsExtensionPoint() {
        IkarosPluginManager ikarosPluginManager = Mockito.mock(IkarosPluginManager.class);
        ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
        ExtensionComponentsFinder extensionComponentsFinder =
            new ExtensionComponentsFinder(ikarosPluginManager, applicationContext);
        Assertions.assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> extensionComponentsFinder.getExtensions(NotExtensionPoint.class))
            .withMessage("The type must be a subclass of ExtensionPoint");
    }

    @Test
    void getExtensions() {
        IkarosPluginManager ikarosPluginManager = Mockito.mock(IkarosPluginManager.class);
        ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
        final ExtensionComponentsFinder extensionComponentsFinder =
            new ExtensionComponentsFinder(ikarosPluginManager, applicationContext);
        UnitTestExtensionPoint pluginManagerUnitComponent = new UnitTestExtensionPoint();
        UnitTestExtensionPoint applicationContextComponent = new UnitTestExtensionPoint();
        Map<String, UnitTestExtensionPoint> beanMap = Mockito.spy(new HashMap<>());
        beanMap.put("unitTestPluginId", applicationContextComponent);

        Mockito.doReturn(List.of(pluginManagerUnitComponent))
            .when(ikarosPluginManager).getExtensions(UnitTestExtensionPoint.class);
        Mockito.doReturn(beanMap)
            .when(applicationContext).getBeansOfType(UnitTestExtensionPoint.class);

        Assertions.assertThat(extensionComponentsFinder.getExtensions(UnitTestExtensionPoint.class))
            .contains(pluginManagerUnitComponent)
            .contains(applicationContextComponent);
    }

    @Test
    void getExtensionsWhenPluginIdIsNull() {
        IkarosPluginManager ikarosPluginManager = Mockito.mock(IkarosPluginManager.class);
        ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
        final ExtensionComponentsFinder extensionComponentsFinder =
            new ExtensionComponentsFinder(ikarosPluginManager, applicationContext);

        UnitTestExtensionPoint applicationContextComponent = new UnitTestExtensionPoint();
        Map<String, UnitTestExtensionPoint> beanMap = Mockito.spy(new HashMap<>());
        beanMap.put("unitTestPluginId", applicationContextComponent);
        Mockito.doReturn(beanMap)
            .when(applicationContext).getBeansOfType(UnitTestExtensionPoint.class);

        Assertions.assertThat(
                extensionComponentsFinder.getExtensions(UnitTestExtensionPoint.class, null))
            .contains(applicationContextComponent);
    }

    @Test
    void getExtensionsWhenPluginIdIsSystemId() {
        IkarosPluginManager ikarosPluginManager = Mockito.mock(IkarosPluginManager.class);
        ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
        final ExtensionComponentsFinder extensionComponentsFinder =
            new ExtensionComponentsFinder(ikarosPluginManager, applicationContext);

        UnitTestExtensionPoint applicationContextComponent = new UnitTestExtensionPoint();
        Map<String, UnitTestExtensionPoint> beanMap = Mockito.spy(new HashMap<>());
        beanMap.put("unitTestPluginId", applicationContextComponent);
        Mockito.doReturn(beanMap)
            .when(applicationContext).getBeansOfType(UnitTestExtensionPoint.class);

        Assertions.assertThat(extensionComponentsFinder.getExtensions(
                UnitTestExtensionPoint.class,
                ExtensionComponentsFinder.SYSTEM_PLUGIN_ID))
            .contains(applicationContextComponent);
    }

    @Test
    void getExtensionsWhenPluginIdIsNotSystemId() {
        IkarosPluginManager ikarosPluginManager = Mockito.mock(IkarosPluginManager.class);
        ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
        final ExtensionComponentsFinder extensionComponentsFinder =
            new ExtensionComponentsFinder(ikarosPluginManager, applicationContext);

        final String pluginId = "unitTestPluginId";
        UnitTestExtensionPoint pluginManagerUnitComponent = new UnitTestExtensionPoint();
        Mockito.doReturn(List.of(pluginManagerUnitComponent))
            .when(ikarosPluginManager).getExtensions(UnitTestExtensionPoint.class, pluginId);


        Assertions.assertThat(extensionComponentsFinder.getExtensions(
                UnitTestExtensionPoint.class, pluginId))
            .contains(pluginManagerUnitComponent);
    }
}