package run.ikaros.server.plugin.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import run.ikaros.server.plugin.PluginApplicationContextRegistry;

@Slf4j
@Component
public class ApplicationReadyEventPluginBroadcaster {
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReadyEvent(ApplicationReadyEvent event) {
        PluginApplicationContextRegistry.getInstance().getPluginApplicationContexts()
            .forEach(pluginApplicationContext -> pluginApplicationContext.publishEvent(event));
    }
}
