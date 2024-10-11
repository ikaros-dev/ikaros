package run.ikaros.server.core.setting;

import reactor.core.publisher.Mono;
import run.ikaros.api.core.setting.vo.GlobalSetting;

public interface SettingService {
    Mono<GlobalSetting> getGlobalSetting();
}
