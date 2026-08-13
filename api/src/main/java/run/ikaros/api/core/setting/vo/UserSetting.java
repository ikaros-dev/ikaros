package run.ikaros.api.core.setting.vo;

import lombok.Data;
import org.jspecify.annotations.Nullable;

@Data
public class UserSetting {
    private @Nullable Boolean allowRegister;
    private @Nullable String defaultRole;
}
