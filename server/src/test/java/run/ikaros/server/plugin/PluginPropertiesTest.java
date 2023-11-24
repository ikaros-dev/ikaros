package run.ikaros.server.plugin;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PluginPropertiesTest {

    @Autowired
    PluginProperties pluginProperties;

    @Test
    void getSystemVersion() {
        Assertions.assertThat(pluginProperties).isNotNull();
        String systemVersion = pluginProperties.getSystemVersion();
        Assertions.assertThat(systemVersion).isNotBlank();
    }
}