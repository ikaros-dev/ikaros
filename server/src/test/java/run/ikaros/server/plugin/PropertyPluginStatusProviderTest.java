package run.ikaros.server.plugin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PropertyPluginStatusProviderTest {

    @Test
    void isPluginDisabled_whenDisabledPluginsContainsId_returnsTrue() {
        PluginProperties props = new PluginProperties();
        props.setDisabledPlugins(new String[]{"plugin-a", "plugin-b"});

        PropertyPluginStatusProvider provider = new PropertyPluginStatusProvider(props);

        assertTrue(provider.isPluginDisabled("plugin-a"));
        assertTrue(provider.isPluginDisabled("plugin-b"));
    }

    @Test
    void isPluginDisabled_whenEnabledPluginsContainsId_returnsFalse() {
        PluginProperties props = new PluginProperties();
        props.setEnabledPlugins(new String[]{"plugin-x", "plugin-y"});

        PropertyPluginStatusProvider provider = new PropertyPluginStatusProvider(props);

        assertFalse(provider.isPluginDisabled("plugin-x"));
        assertFalse(provider.isPluginDisabled("plugin-y"));
    }

    @Test
    void isPluginDisabled_whenEnabledPluginsNotEmpty_andIdNotInEnabled_returnsTrue() {
        PluginProperties props = new PluginProperties();
        props.setEnabledPlugins(new String[]{"plugin-x"});

        PropertyPluginStatusProvider provider = new PropertyPluginStatusProvider(props);

        assertTrue(provider.isPluginDisabled("plugin-z"));
    }

    @Test
    void isPluginDisabled_whenBothEmpty_returnsFalse() {
        PluginProperties props = new PluginProperties();
        props.setEnabledPlugins(new String[0]);
        props.setDisabledPlugins(new String[0]);

        PropertyPluginStatusProvider provider = new PropertyPluginStatusProvider(props);

        assertFalse(provider.isPluginDisabled("any-plugin"));
    }

    @Test
    void disablePlugin_addsToDisabledAndRemovesFromEnabled() {
        PluginProperties props = new PluginProperties();
        props.setEnabledPlugins(new String[]{"plugin-a"});

        PropertyPluginStatusProvider provider = new PropertyPluginStatusProvider(props);
        assertFalse(provider.isPluginDisabled("plugin-a"));

        provider.disablePlugin("plugin-a");
        assertTrue(provider.isPluginDisabled("plugin-a"));
    }

    @Test
    void enablePlugin_addsToEnabledAndRemovesFromDisabled() {
        PluginProperties props = new PluginProperties();
        props.setDisabledPlugins(new String[]{"plugin-a"});

        PropertyPluginStatusProvider provider = new PropertyPluginStatusProvider(props);
        assertTrue(provider.isPluginDisabled("plugin-a"));

        provider.enablePlugin("plugin-a");
        assertFalse(provider.isPluginDisabled("plugin-a"));
    }

    @Test
    void disablePlugin_alreadyDisabled_doesNothing() {
        PluginProperties props = new PluginProperties();
        props.setDisabledPlugins(new String[]{"plugin-a"});

        PropertyPluginStatusProvider provider = new PropertyPluginStatusProvider(props);
        provider.disablePlugin("plugin-a");
        assertTrue(provider.isPluginDisabled("plugin-a"));
    }

    @Test
    void enablePlugin_alreadyEnabled_doesNothing() {
        PluginProperties props = new PluginProperties();
        props.setEnabledPlugins(new String[]{"plugin-a"});

        PropertyPluginStatusProvider provider = new PropertyPluginStatusProvider(props);
        provider.enablePlugin("plugin-a");
        assertFalse(provider.isPluginDisabled("plugin-a"));
    }

    @Test
    void nullEnabledPlugins_handledGracefully() {
        PluginProperties props = new PluginProperties();
        props.setEnabledPlugins(null);
        props.setDisabledPlugins(new String[]{"plugin-a"});

        PropertyPluginStatusProvider provider = new PropertyPluginStatusProvider(props);
        assertTrue(provider.isPluginDisabled("plugin-a"));
        assertFalse(provider.isPluginDisabled("plugin-b"));
    }

    @Test
    void nullDisabledPlugins_handledGracefully() {
        PluginProperties props = new PluginProperties();
        props.setDisabledPlugins(null);
        props.setEnabledPlugins(new String[]{"plugin-a"});

        PropertyPluginStatusProvider provider = new PropertyPluginStatusProvider(props);
        assertFalse(provider.isPluginDisabled("plugin-a"));
    }
}
