package run.ikaros.server.core.setting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.setting.ConfigMap;
import run.ikaros.api.custom.ReactiveCustomClient;

class DefaultSettingServiceTest {
    private ReactiveCustomClient reactiveCustomClient;
    private DefaultSettingService defaultSettingService;

    @BeforeEach
    void setUp() {
        reactiveCustomClient = Mockito.mock(ReactiveCustomClient.class);
        defaultSettingService = new DefaultSettingService(reactiveCustomClient);
    }

    @Test
    void getGlobalSetting_withHeaderAndFooter() {
        ConfigMap configMap = new ConfigMap();
        configMap.putDataItem("GLOBAL_HEADER", "site-header");
        configMap.putDataItem("GLOBAL_FOOTER", "site-footer");
        when(reactiveCustomClient.findOne(any(), any()))
            .thenReturn(Mono.just(configMap));

        StepVerifier.create(defaultSettingService.getGlobalSetting())
            .assertNext(setting -> {
                assertThat(setting.getHeader()).isEqualTo("site-header");
                assertThat(setting.getFooter()).isEqualTo("site-footer");
            })
            .verifyComplete();
    }

    @Test
    void getGlobalSetting_withoutHeaderAndFooter() {
        ConfigMap configMap = new ConfigMap();
        configMap.putDataItem("OTHER_KEY", "value");
        when(reactiveCustomClient.findOne(any(), any()))
            .thenReturn(Mono.just(configMap));

        StepVerifier.create(defaultSettingService.getGlobalSetting())
            .assertNext(setting -> {
                assertThat(setting.getHeader()).isNull();
                assertThat(setting.getFooter()).isNull();
            })
            .verifyComplete();
    }

    @Test
    void getGlobalSetting_whenConfigMapNotFound() {
        when(reactiveCustomClient.findOne(any(), any()))
            .thenReturn(Mono.empty());

        StepVerifier.create(defaultSettingService.getGlobalSetting())
            .verifyComplete();
    }

    @Test
    void getGlobalSetting_withEmptyData() {
        ConfigMap configMap = new ConfigMap();
        configMap.setData(Map.of());
        when(reactiveCustomClient.findOne(any(), any()))
            .thenReturn(Mono.just(configMap));

        StepVerifier.create(defaultSettingService.getGlobalSetting())
            .assertNext(setting -> {
                assertThat(setting.getHeader()).isNull();
                assertThat(setting.getFooter()).isNull();
            })
            .verifyComplete();
    }
}
