package run.ikaros.server.plugin;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class PluginApplicationContextTest {

    @Test
    void getPluginId() {
        String pluginId = "TestPluginId";
        PluginApplicationContext pluginApplicationContext = new PluginApplicationContext(pluginId);
        Assertions.assertThat(pluginApplicationContext.getPluginId())
            .isEqualTo(pluginId);
    }
}