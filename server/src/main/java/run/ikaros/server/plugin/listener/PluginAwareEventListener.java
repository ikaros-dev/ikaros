package run.ikaros.server.plugin.listener;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import run.ikaros.api.plugin.event.PluginAwareEvent;
import run.ikaros.server.plugin.IkarosPluginManager;
import run.ikaros.server.plugin.PluginApplicationContext;
import run.ikaros.server.plugin.PluginApplicationContextRegistry;

@Slf4j
@Component
public class PluginAwareEventListener {
    private final IkarosPluginManager ikarosPluginManager;

    public PluginAwareEventListener(IkarosPluginManager ikarosPluginManager) {
        this.ikarosPluginManager = ikarosPluginManager;
    }

    /**
     * Notify plugin event.
     */
    @EventListener(PluginAwareEvent.class)
    public void notifyPlugin(PluginAwareEvent event) {
        if (Objects.isNull(event)) {
            return;
        }
        String pluginId = event.getPluginId();
        if ("ALL".equals(pluginId)) {
            log.debug("publish event [{}] to plugin [{}].",
                event.getClass().getName(), pluginId);
            for (PluginApplicationContext pluginApplicationContext :
                PluginApplicationContextRegistry.getInstance()
                    .getPluginApplicationContexts()) {
                PluginWrapper pluginWrapper =
                    ikarosPluginManager.getPlugin(pluginApplicationContext.getPluginId());
                PluginState pluginState = pluginWrapper.getPluginState();
                if (!PluginState.STARTED.equals(pluginState)) {
                    continue;
                }
                pluginApplicationContext.publishEvent(event);
            }
        } else {
            PluginWrapper pluginWrapper = ikarosPluginManager.getPlugin(pluginId);
            PluginState pluginState = pluginWrapper.getPluginState();
            if (!PluginState.STARTED.equals(pluginState)) {
                return;
            }
            log.debug("publish event [{}] to plugin [{}].",
                event.getClass().getName(), pluginId);
            PluginApplicationContextRegistry.getInstance().getByPluginId(pluginId)
                .publishEvent(event);
        }
    }
}
