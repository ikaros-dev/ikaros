package run.ikaros.api.core.setting.vo;

import lombok.Data;

@Data
public class UserSetting {
    private Boolean allowRegister;
    private String defaultRole;
}
