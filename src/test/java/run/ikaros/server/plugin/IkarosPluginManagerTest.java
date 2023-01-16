package run.ikaros.server.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.pf4j.PluginAlreadyLoadedException;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import run.ikaros.server.plugin.event.IkarosPluginStoppedEvent;
import run.ikaros.server.test.reflect.MemberMatcher;

@Slf4j
@SpringBootTest
class IkarosPluginManagerTest {

    @Autowired
    IkarosPluginManager ikarosPluginManager;

    Path parentPluginPath;
    Path childPluginPath;
    Path pluginPath;
    String notExistsPluginId = "notExistsPluginId";
    String unitTestPluginResourcePrefix = "plugin";
    String unitTestParentPlugin = unitTestPluginResourcePrefix + "/unittest-a-1.0.0.jar";
    String unitTestChildPlugin = unitTestPluginResourcePrefix + "/unittest-b-1.0.0.jar";

    @BeforeEach
    void setUp() throws URISyntaxException {
        parentPluginPath = Path.of(Objects.requireNonNull(
                getClass().getClassLoader().getResource(unitTestParentPlugin))
            .toURI());
        childPluginPath = Path.of(Objects.requireNonNull(
                getClass().getClassLoader().getResource(unitTestChildPlugin))
            .toURI());
        pluginPath = parentPluginPath;
        // pluginPath = Path.of("C:\\Develop\\ikaros-dev\\dev-plugins\\plugin-starter");
    }

    @AfterEach
    void tearDown() {
        ikarosPluginManager.unloadPlugins();
        ikarosPluginManager.clearPluginStaringError();
    }

    private String loadPluginIfNotLoaded(Path pluginPath) {
        try {
            return ikarosPluginManager.loadPlugin(pluginPath);
        } catch (PluginAlreadyLoadedException e) {
            // ignore plugin already loaded when unit test
            log.debug("plugin [{}] already loaded", e.getPluginId());
            return e.getPluginId();
        }
    }


    @Test
    void startPlugin() {
        // when normal
        String pluginId = loadPluginIfNotLoaded(pluginPath);
        assertThat(ikarosPluginManager.startPlugin(pluginId))
            .isEqualByComparingTo(PluginState.STARTED);
        ikarosPluginManager.stopPlugin(pluginId);

        // when check pluginId fail such as pluginId not exists
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> ikarosPluginManager.startPlugin(notExistsPluginId))
            .withMessageContaining("Unknown pluginId %s", notExistsPluginId);

        // when plugin has started
        assertThat(ikarosPluginManager.startPlugin(pluginId))
            .isEqualByComparingTo(PluginState.STARTED);
        assertThat(ikarosPluginManager.startPlugin(pluginId))
            .isEqualByComparingTo(PluginState.STARTED);
        ikarosPluginManager.stopPlugin(pluginId);

        assertThat(ikarosPluginManager.disablePlugin(pluginId)).isTrue();
        ikarosPluginManager.startPlugin(pluginId);
    }

    @Test
    void startPluginWhenStatusIsCreated() {
        // when plugin is not resolved (status is created)
        String pluginId = loadPluginIfNotLoaded(pluginPath);
        ikarosPluginManager.startPlugin(pluginId);
        ikarosPluginManager.unloadPlugin(pluginId);
        ikarosPluginManager.loadPluginFromPath(pluginPath);
        assertThat(ikarosPluginManager.getPlugin(pluginId).getPluginState())
            .isEqualByComparingTo(PluginState.CREATED);
        ikarosPluginManager.startPlugin(pluginId);
        assertThat(ikarosPluginManager.getPlugin(pluginId).getPluginState())
            .isEqualByComparingTo(PluginState.CREATED);
        ikarosPluginManager.resolvePlugins();
        ikarosPluginManager.unloadPlugin(pluginId);
        loadPluginIfNotLoaded(pluginPath);
    }

    @Test
    void startPluginThatHasDependency() throws URISyntaxException {
        String parentPluginId = loadPluginIfNotLoaded(parentPluginPath);
        String childPluginId = loadPluginIfNotLoaded(childPluginPath);
        ikarosPluginManager.startPlugin(childPluginId);
        PluginWrapper parentPluginWrapper = ikarosPluginManager.getPlugin(parentPluginId);
        PluginWrapper childPluginWrapper = ikarosPluginManager.getPlugin(childPluginId);
        assertThat(parentPluginWrapper.getPluginState()).isEqualByComparingTo(PluginState.STARTED);
        assertThat(childPluginWrapper.getPluginState()).isEqualByComparingTo(PluginState.STARTED);
    }

    @Test
    void startPlugins() {
        loadPluginIfNotLoaded(parentPluginPath);
        loadPluginIfNotLoaded(childPluginPath);
        ikarosPluginManager.startPlugins();
    }

    @Test
    void getPluginApplicationContext() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> ikarosPluginManager
                .getPluginApplicationContext(notExistsPluginId))
            .withMessageContaining("The plugin [%s] can not be found.", notExistsPluginId);

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> ikarosPluginManager
                .getPluginApplicationContext(notExistsPluginId))
            .withMessageContaining("The plugin [%s] can not be found.", notExistsPluginId);

        String pluginId = loadPluginIfNotLoaded(pluginPath);
        ikarosPluginManager.startPlugin(pluginId);
        assertThat(ikarosPluginManager.getPluginApplicationContext(pluginId))
            .isNotNull();
    }

    @Test
    void destroy() {
        ikarosPluginManager.destroy();
    }

    @Test
    void getPluginStartingError() {
        assertThat(ikarosPluginManager.getPluginStartingError(notExistsPluginId))
            .isNull();
    }

    @Test
    void getPluginRepository() {
        assertThat(ikarosPluginManager.getPluginRepository())
            .isNotNull();
    }

    @Test
    void stopPlugin() throws NoSuchFieldException, IllegalAccessException {
        // when plugin has stopped
        String pluginId = loadPluginIfNotLoaded(pluginPath);
        ikarosPluginManager.startPlugin(pluginId);
        ikarosPluginManager.stopPlugin(pluginId);
        assertThat(ikarosPluginManager.stopPlugin(pluginId))
            .isEqualByComparingTo(PluginState.STOPPED);

        // when plugin has disabled
        ikarosPluginManager.startPlugin(pluginId);
        ikarosPluginManager.disablePlugin(pluginId);
        assertThat(ikarosPluginManager.stopPlugin(pluginId))
            .isEqualByComparingTo(PluginState.DISABLED);


        // when call pluginApplicationInitializer#onStartUp(pluginId) throw exception
        ikarosPluginManager.startPlugin(pluginId);
        assertThat(ikarosPluginManager.getPluginStartingError(pluginId)).isNull();

        Field rootApplicationContextField =
            MemberMatcher.field(IkarosPluginManager.class, "rootApplicationContext");
        ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
        RuntimeException expectException = new RuntimeException("mock exception");
        Mockito.doThrow(expectException)
            .when(applicationContext).publishEvent(Mockito.any(IkarosPluginStoppedEvent.class));

        Object originalFieldValue = rootApplicationContextField.get(ikarosPluginManager);
        try {
            rootApplicationContextField.set(ikarosPluginManager, applicationContext);
            ikarosPluginManager.stopPlugin(pluginId);
            assertThat(ikarosPluginManager.getPluginStartingError(pluginId)).isNotNull();
        } finally {
            rootApplicationContextField.set(ikarosPluginManager, originalFieldValue);
        }

        // when plugin has dependency
        // String parentPluginId = loadPluginIfNotLoaded(parentPluginPath);
        // String childPluginId = loadPluginIfNotLoaded(childPluginPath);
        // PluginWrapper childPluginWrapper = ikarosPluginManager.getPlugin(childPluginId);
        // ikarosPluginManager.startPlugin(childPluginId);
        // assertThat(ikarosPluginManager.getPlugin(parentPluginId).getPluginState())
        //     .isEqualByComparingTo(PluginState.STARTED);
        // assertThat(childPluginWrapper.getPluginState())
        //     .isEqualByComparingTo(PluginState.STARTED);
        //
        // ikarosPluginManager.stopPlugin(childPluginId);
        // assertThat(ikarosPluginManager.getPlugin(parentPluginId).getPluginState())
        //     .isEqualByComparingTo(PluginState.STOPPED);
        // assertThat(childPluginWrapper.getPluginState())
        //     .isEqualByComparingTo(PluginState.STOPPED);
    }
}