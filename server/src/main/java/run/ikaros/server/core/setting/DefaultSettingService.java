package run.ikaros.server.core.setting;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.setting.ConfigMap;
import run.ikaros.api.core.setting.vo.GlobalSetting;
import run.ikaros.api.custom.ReactiveCustomClient;

@Slf4j
@Service
public class DefaultSettingService implements SettingService {

    private final ReactiveCustomClient reactiveCustomClient;
    private static final String configMapName = "setting.server.ikaros.run";

    public DefaultSettingService(ReactiveCustomClient reactiveCustomClient) {
        this.reactiveCustomClient = reactiveCustomClient;
    }

    @Override
    public Mono<GlobalSetting> getGlobalSetting() {
        return reactiveCustomClient.findOne(ConfigMap.class, configMapName)
            .map(configMap -> {
                Map<String, String> data = configMap.getData();
                GlobalSetting globalSetting = new GlobalSetting();
                globalSetting.setHeader(data.get("GLOBAL_HEADER"));
                globalSetting.setFooter(data.get("GLOBAL_FOOTER"));
                return globalSetting;
            });
    }
}
