package run.ikaros.api.core.setting.vo;

import lombok.Data;
import org.jspecify.annotations.Nullable;

@Data
public class BasicSetting {
    private @Nullable String siteTitle;
    private @Nullable String siteSubhead;
    private @Nullable String logo;
    private @Nullable String favicon;
}
