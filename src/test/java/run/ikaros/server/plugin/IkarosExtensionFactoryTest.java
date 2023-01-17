package run.ikaros.server.plugin;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

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


}