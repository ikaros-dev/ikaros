package run.ikaros.server.theme;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.setting.ConfigMap;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.server.core.setting.SystemSettingInitListener;
import run.ikaros.server.infra.constants.SettingKeyConst;
import run.ikaros.server.infra.constants.ThemeConst;

@Slf4j
@Service
public class DefaultThemeService implements ThemeService {
    private final ReactiveCustomClient reactiveCustomClient;

    public DefaultThemeService(ReactiveCustomClient reactiveCustomClient) {
        this.reactiveCustomClient = reactiveCustomClient;
    }

    @Override
    public Mono<String> getCurrentTheme() {
        return reactiveCustomClient.findOne(ConfigMap.class,
                SystemSettingInitListener.getConfigMapName())
            .map(ConfigMap::getData)
            .map(map -> map.getOrDefault(SettingKeyConst.THEME_SELECT, ThemeConst.DEFAULT));
    }
}
