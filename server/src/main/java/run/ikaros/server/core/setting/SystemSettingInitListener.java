package run.ikaros.server.core.setting;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.SecurityConst;
import run.ikaros.api.core.setting.ConfigMap;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.exception.NotFoundException;
import run.ikaros.server.custom.scheme.SchemeInitializedEvent;

@Slf4j
@Component
public class SystemSettingInitListener {
    private final ReactiveCustomClient reactiveCustomClient;

    public SystemSettingInitListener(ReactiveCustomClient reactiveCustomClient) {
        this.reactiveCustomClient = reactiveCustomClient;
    }

    /**
     * Init add system default config items.
     */
    @EventListener(SchemeInitializedEvent.class)
    public Mono<Void> handle(SchemeInitializedEvent event) {
        final var configMapName = "setting.server.ikaros.run";
        var settingConfigMap = new ConfigMap();
        settingConfigMap.setName(configMapName);

        // System basic settings
        settingConfigMap.putDataItem("SITE_TITLE", "");
        settingConfigMap.putDataItem("SITE_SUBHEAD", "");
        settingConfigMap.putDataItem("LOGO", "");
        settingConfigMap.putDataItem("FAVICON", "");

        // System mail settings
        settingConfigMap.putDataItem("MAIL_ENABLE", "false");
        settingConfigMap.putDataItem("MAIL_PROTOCOL", "smtp");
        settingConfigMap.putDataItem("MAIL_SMTP_HOST", "");
        settingConfigMap.putDataItem("MAIL_SMTP_PORT", "");
        settingConfigMap.putDataItem("MAIL_SMTP_ACCOUNT", "");
        settingConfigMap.putDataItem("MAIL_SMTP_PASSWORD", "");
        settingConfigMap.putDataItem("MAIL_SMTP_ACCOUNT_ALIAS", "");

        // System user settings
        settingConfigMap.putDataItem("ALLOW_REGISTER", "false");
        settingConfigMap.putDataItem("DEFAULT_ROLE", SecurityConst.AnonymousUser.Role);

        // System global settings
        settingConfigMap.putDataItem("GLOBAL_HEADER", "");
        settingConfigMap.putDataItem("GLOBAL_FOOTER", "");

        // System remote settings
        settingConfigMap.putDataItem("REMOTE_ENABLE", "false");

        return reactiveCustomClient.findOne(ConfigMap.class, configMapName)
            .onErrorResume(NotFoundException.class, e ->
                reactiveCustomClient.create(settingConfigMap)
                    .doOnSuccess(cm -> log.debug("Create init setting config map: {}", cm)))
            .flatMap(configMap -> {
                Map<String, String> map = configMap.getData();
                for (Map.Entry<String, String> entry : settingConfigMap.getData().entrySet()) {
                    if (!map.containsKey(entry.getKey())) {
                        map.put(entry.getKey(), entry.getValue());
                        log.info("add new item for setting config map, key={}, value={}",
                            entry.getKey(), entry.getValue());
                    }
                }
                return reactiveCustomClient.update(configMap);
            })
            .then();
    }

}
