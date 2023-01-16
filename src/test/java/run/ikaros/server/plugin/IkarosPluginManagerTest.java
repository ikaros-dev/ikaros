package run.ikaros.server.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static run.ikaros.server.test.TestConst.PROCESS_SHOULD_NOT_RUN_TO_THIS;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pf4j.PluginState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
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
    void startPlugin() {
        // when normal
        String pluginId = ikarosPluginManager.loadPlugin(pluginPath);
        PluginState pluginState = ikarosPluginManager.startPlugin(pluginId);
        assertThat(pluginState).isEqualByComparingTo(PluginState.STARTED);
        ikarosPluginManager.stopPlugin(pluginId);

        // when check pluginId fail such as pluginId not exists
        try {
            ikarosPluginManager.startPlugin("notExistsPluginIdValue");
            fail(PROCESS_SHOULD_NOT_RUN_TO_THIS);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("Unknown pluginId");
        }

        // when plugin has started
        pluginState = ikarosPluginManager.startPlugin(pluginId);
        assertThat(pluginState).isEqualByComparingTo(PluginState.STARTED);
        pluginState = ikarosPluginManager.startPlugin(pluginId);
        assertThat(pluginState).isEqualByComparingTo(PluginState.STARTED);
        ikarosPluginManager.stopPlugin(pluginId);

        assertThat(ikarosPluginManager.disablePlugin(pluginId)).isTrue();
        ikarosPluginManager.startPlugin(pluginId);

    }

    @Test
    void getPluginApplicationContext() {
    }

    @Test
    void getPluginStartingError() {
    }

    @Test
    void stopPlugin() {
    }

    @Test
    void startPlugins() {
    }

    @Test
    void testStartPlugin() {
    }

    @Test
    void stopPlugins() {
    }

    @Test
    void reloadPlugins() {
    }

    @Test
    void reloadPlugin() {
    }

    @Test
    void releaseAdditionalResources() {
    }

}