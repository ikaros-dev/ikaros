package run.ikaros.server.plugin.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import run.ikaros.api.plugin.event.PluginAwareEvent;
import run.ikaros.server.plugin.PluginApplicationContext;
import run.ikaros.server.plugin.PluginApplicationContextRegistry;

@Slf4j
@Component
public class PluginAwareEventListener {
    /**
     * Notify plugin event.
     */
    @EventListener(PluginAwareEvent.class)
    public void notifyPlugin(PluginAwareEvent event) {
        String pluginId = event.getPluginId();
        if ("ALL".equals(pluginId)) {
            log.debug("publish event [{}] to plugin [{}].",
                event.getClass().getName(), pluginId);
            for (PluginApplicationContext pluginApplicationContext :
                PluginApplicationContextRegistry.getInstance()
                    .getPluginApplicationContexts()) {
                pluginApplicationContext.publishEvent(event);
            }
        } else {
            log.debug("publish event [{}] to plugin [{}].",
                event.getClass().getName(), pluginId);
            PluginApplicationContextRegistry.getInstance().getByPluginId(pluginId)
                .publishEvent(event);
        }
    }
}
