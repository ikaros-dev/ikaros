package run.ikaros.server.plugin;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.pf4j.PluginManager;

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
            = new IkarosExtensionFactory(Mockito.any(PluginManager.class), false);
        Assertions.assertThat(ikarosExtensionFactory.create(CanNewInstance.class))
            .isInstanceOf(CanNewInstance.class);
    }

    @Test
    void createNotUseSpringAndHasReflectException() {
        IkarosExtensionFactory ikarosExtensionFactory
            = new IkarosExtensionFactory(Mockito.any(PluginManager.class), false);
        Assertions.assertThatExceptionOfType(PluginException.class)
            .isThrownBy(() -> ikarosExtensionFactory.create(NotNewInstance.class))
            .withMessageContaining("new extension cls instance fail");
    }


}