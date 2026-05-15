package run.ikaros.server.plugin.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import run.ikaros.api.core.setting.ConfigMap;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.custom.scheme.CustomScheme;
import run.ikaros.api.plugin.event.PluginConfigMapCreateEvent;
import run.ikaros.server.custom.event.CustomCreateEvent;

@Slf4j
@Component
public class ConfigMapCustomCreateEventListener {

    private final ReactiveCustomClient reactiveCustomClient;
    private final ApplicationEventPublisher applicationEventPublisher;

    public ConfigMapCustomCreateEventListener(ReactiveCustomClient reactiveCustomClient,
                                              ApplicationEventPublisher applicationEventPublisher) {
        this.reactiveCustomClient = reactiveCustomClient;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Handle {@link CustomCreateEvent}.
     */
    @EventListener(CustomCreateEvent.class)
    public void onCustomUpdate(CustomCreateEvent event) {
        CustomScheme scheme = event.getScheme();
        String name = event.getName();
        if (ConfigMap.class != scheme.type()) {
            return;
        }
        reactiveCustomClient.findOne(scheme.type(), name)
            .map(custom -> (ConfigMap) custom)
            .subscribe(configMap -> applicationEventPublisher.publishEvent(
                new PluginConfigMapCreateEvent(this, name, configMap)));
    }
}
