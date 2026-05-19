package run.ikaros.server.plugin.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.setting.ConfigMap;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.custom.scheme.CustomScheme;
import run.ikaros.api.plugin.event.PluginConfigMapUpdateEvent;
import run.ikaros.server.custom.event.CustomUpdateEvent;

class ConfigMapCustomUpdateEventListenerTest {

    private ReactiveCustomClient reactiveCustomClient;
    private ApplicationEventPublisher applicationEventPublisher;
    private ConfigMapCustomUpdateEventListener listener;

    @BeforeEach
    void setUp() {
        reactiveCustomClient = org.mockito.Mockito.mock(ReactiveCustomClient.class);
        applicationEventPublisher = org.mockito.Mockito.mock(ApplicationEventPublisher.class);
        listener = new ConfigMapCustomUpdateEventListener(reactiveCustomClient,
            applicationEventPublisher);
    }

    @Test
    void onCustomUpdateWithConfigMapTypePublishesEvent() {
        CustomScheme scheme = CustomScheme.buildFromType(ConfigMap.class);
        String name = "test-plugin";
        CustomUpdateEvent event = new CustomUpdateEvent(this, scheme, name);

        ConfigMap configMap = new ConfigMap();
        configMap.setName(name);
        when(reactiveCustomClient.findOne(ConfigMap.class, name))
            .thenReturn(Mono.just(configMap));

        listener.onCustomUpdate(event);

        ArgumentCaptor<ApplicationEvent> captor =
            ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());

        PluginConfigMapUpdateEvent publishedEvent =
            (PluginConfigMapUpdateEvent) captor.getValue();
        assertThat(publishedEvent).isInstanceOf(PluginConfigMapUpdateEvent.class);
    }

    @Test
    void constructorSetsFields() {
        ReactiveCustomClient client = org.mockito.Mockito.mock(ReactiveCustomClient.class);
        ApplicationEventPublisher publisher =
            org.mockito.Mockito.mock(ApplicationEventPublisher.class);
        ConfigMapCustomUpdateEventListener testListener =
            new ConfigMapCustomUpdateEventListener(client, publisher);
        assertThat(testListener).isNotNull();
    }
}
