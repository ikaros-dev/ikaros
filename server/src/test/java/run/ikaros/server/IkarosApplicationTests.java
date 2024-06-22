package run.ikaros.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import run.ikaros.api.infra.properties.IkarosProperties;
import run.ikaros.server.plugin.PluginProperties;

@SpringBootTest
class IkarosApplicationTests {
    @Autowired
    IkarosProperties ikarosProperties;
    @Autowired
    PluginProperties pluginProperties;

    @Test
    void contextLoads() {
    }

    @Test
    void ikarosProps() {
        assertThat(ikarosProperties).isNotNull();
    }
}
