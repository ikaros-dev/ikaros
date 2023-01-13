package run.ikaros.server.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.pf4j.PluginState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled
class IkarosPluginManagerTest {

    @Autowired
    IkarosPluginManager ikarosPluginManager;

    Path pluginPath;

    @BeforeEach
    void setUp() throws URISyntaxException {
        pluginPath = Path.of(Objects.requireNonNull(
                getClass().getClassLoader().getResource("plugin/plugin-starter-1.0-SNAPSHOT.jar"))
            .toURI());
        // pluginPath = Path.of("C:\\Develop\\ikaros-dev\\plugin-starter");
    }

    @AfterEach
    void tearDown() {
        ikarosPluginManager.stopPlugins();
    }

    @Test
    void loadPlugin() {
        String pluginId = ikarosPluginManager.loadPlugin(pluginPath);
        assertThat(pluginId).isNotBlank();
        ikarosPluginManager.disablePlugin(pluginId);
    }


    @Test
    void startPlugin() {
        String pluginId = ikarosPluginManager.loadPlugin(pluginPath);
        PluginState pluginState = ikarosPluginManager.startPlugin(pluginId);
        assertThat(pluginState).isEqualByComparingTo(PluginState.STARTED);
        ikarosPluginManager.disablePlugin(pluginId);
        ikarosPluginManager.stopPlugin(pluginId);
    }
}