package run.ikaros.server.plugin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginStatusProvider;

/**
 * <p>An implementation for {@link PluginStatusProvider}.</p>
 * <p>
 * The enabled plugins are read from {@code ikaros.plugin.enabled-plugins} properties
 * in <code>application.yaml</code>.
 * </p>
 * <p>
 * The disabled plugins are read from {@code ikaros.plugin.disabled-plugins} properties
 * in <code>application.yaml</code>.
 * </p>
 */
@Slf4j
public class PropertyPluginStatusProvider implements PluginStatusProvider {
    private final Set<String> enabledPlugins;
    private final Set<String> disabledPlugins;

    /**
     * Construct instance by {@link PluginProperties}.
     *
     * @param pluginProperties plugin props
     */
    public PropertyPluginStatusProvider(PluginProperties pluginProperties) {
        this.enabledPlugins = new HashSet<>();
        if (Objects.nonNull(pluginProperties.getEnabledPlugins())) {
            this.enabledPlugins.addAll(Arrays.stream(pluginProperties.getEnabledPlugins())
                .collect(Collectors.toSet()));
        }
        log.info("Enabled plugins: {}", enabledPlugins);

        this.disabledPlugins = new HashSet<>();
        if (Objects.nonNull(pluginProperties.getDisabledPlugins())) {
            this.disabledPlugins.addAll(Arrays.stream(pluginProperties.getDisabledPlugins())
                .collect(Collectors.toSet()));
        }
        log.info("Disabled plugins: {}", disabledPlugins);
    }

    @Override
    public boolean isPluginDisabled(String pluginId) {
        if (disabledPlugins.contains(pluginId)) {
            return true;
        }
        return !enabledPlugins.isEmpty() && !enabledPlugins.contains(pluginId);
    }

    @Override
    public void disablePlugin(String pluginId) {
        if (isPluginDisabled(pluginId)) {
            return;
        }
        disabledPlugins.add(pluginId);
        enabledPlugins.remove(pluginId);
    }

    @Override
    public void enablePlugin(String pluginId) {
        if (!isPluginDisabled(pluginId)) {
            return;
        }
        enabledPlugins.add(pluginId);
        disabledPlugins.remove(pluginId);
    }
}
