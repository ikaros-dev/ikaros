package run.ikaros.server.plugin.listener;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IkarosPluginDeleteEventListenerTest {

    @Test
    void constructorSetsReactiveCustomClient() {
        // Verify listener can be instantiated
        var client = org.mockito.Mockito.mock(
            run.ikaros.api.custom.ReactiveCustomClient.class);
        var listener = new IkarosPluginDeleteEventListener(client);
        assertThat(listener).isNotNull();
    }
}
