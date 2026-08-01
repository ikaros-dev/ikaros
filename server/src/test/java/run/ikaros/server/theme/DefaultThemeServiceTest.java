package run.ikaros.server.theme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.setting.ConfigMap;
import run.ikaros.api.custom.ReactiveCustomClient;

class DefaultThemeServiceTest {
    private ReactiveCustomClient reactiveCustomClient;
    private DefaultThemeService defaultThemeService;

    @BeforeEach
    void setUp() {
        reactiveCustomClient = Mockito.mock(ReactiveCustomClient.class);
        defaultThemeService = new DefaultThemeService(reactiveCustomClient);
    }

    @Test
    void getCurrentTheme_whenThemeSet() {
        ConfigMap configMap = new ConfigMap();
        configMap.putDataItem("THEME_SELECT", "dark");
        when(reactiveCustomClient.findOne(any(), any()))
            .thenReturn(Mono.just(configMap));

        StepVerifier.create(defaultThemeService.getCurrentTheme())
            .assertNext(theme -> assertThat(theme).isEqualTo("dark"))
            .verifyComplete();
    }

    @Test
    void getCurrentTheme_whenThemeNotSet_returnsDefault() {
        ConfigMap configMap = new ConfigMap();
        configMap.putDataItem("OTHER_KEY", "value");
        when(reactiveCustomClient.findOne(any(), any()))
            .thenReturn(Mono.just(configMap));

        StepVerifier.create(defaultThemeService.getCurrentTheme())
            .assertNext(theme -> assertThat(theme).isEqualTo("simple"))
            .verifyComplete();
    }

    @Test
    void getCurrentTheme_whenConfigMapNotFound_returnsEmpty() {
        when(reactiveCustomClient.findOne(any(), any()))
            .thenReturn(Mono.empty());

        StepVerifier.create(defaultThemeService.getCurrentTheme())
            .verifyComplete();
    }

    @Test
    void getCurrentTheme_whenDataIsNull_throwsNpe() {
        ConfigMap configMap = new ConfigMap();
        configMap.setData(null);
        when(reactiveCustomClient.findOne(any(), any()))
            .thenReturn(Mono.just(configMap));

        StepVerifier.create(defaultThemeService.getCurrentTheme())
            .expectError(NullPointerException.class)
            .verify();
    }
}
