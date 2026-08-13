package run.ikaros.api.core.setting.vo;

import lombok.Data;
import org.jspecify.annotations.Nullable;

@Data
public class GlobalSetting {
    private @Nullable String header;
    private @Nullable String footer;
}
