package run.ikaros.server.plugin.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import run.ikaros.api.plugin.custom.Plugin;
import run.ikaros.api.plugin.event.PluginConfigMapCreateEvent;
import run.ikaros.server.custom.event.CustomCreateEvent;

class ConfigMapCustomCreateEventListenerTest {

    private ReactiveCustomClient reactiveCustomClient;
    private ApplicationEventPublisher applicationEventPublisher;
    private ConfigMapCustomCreateEventListener listener;

    @BeforeEach
    void setUp() {
        reactiveCustomClient = org.mockito.Mockito.mock(ReactiveCustomClient.class);
        applicationEventPublisher = org.mockito.Mockito.mock(ApplicationEventPublisher.class);
        listener = new ConfigMapCustomCreateEventListener(reactiveCustomClient,
            applicationEventPublisher);
    }

    @Test
    void onCustomUpdateWithConfigMapTypePublishesEvent() {
        CustomScheme scheme = CustomScheme.buildFromType(ConfigMap.class);
        String name = "test-plugin";
        CustomCreateEvent event = new CustomCreateEvent(this, scheme, name);

        ConfigMap configMap = new ConfigMap();
        configMap.setName(name);
        when(reactiveCustomClient.findOne(ConfigMap.class, name))
            .thenReturn(Mono.just(configMap));

        listener.onCustomUpdate(event);

        ArgumentCaptor<ApplicationEvent> captor =
            ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());

        PluginConfigMapCreateEvent publishedEvent =
            (PluginConfigMapCreateEvent) captor.getValue();
        assertThat(publishedEvent).isInstanceOf(PluginConfigMapCreateEvent.class);
    }

    @Test
    void onCustomUpdateWithNonConfigMapTypeReturnsEarly() {
        CustomScheme scheme = CustomScheme.buildFromType(Plugin.class);
        String name = "some-name";
        CustomCreateEvent event = new CustomCreateEvent(this, scheme, name);

        listener.onCustomUpdate(event);

        // Non-ConfigMap type, so findOne should NOT be called
        verify(reactiveCustomClient, never()).findOne(any(), any());
    }

    @Test
    void constructorSetsFields() {
        ReactiveCustomClient client = org.mockito.Mockito.mock(ReactiveCustomClient.class);
        ApplicationEventPublisher publisher =
            org.mockito.Mockito.mock(ApplicationEventPublisher.class);
        ConfigMapCustomCreateEventListener testListener =
            new ConfigMapCustomCreateEventListener(client, publisher);
        assertThat(testListener).isNotNull();
    }
}
