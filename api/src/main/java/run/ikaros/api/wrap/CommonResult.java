package run.ikaros.api.wrap;

import lombok.Data;
import org.jspecify.annotations.Nullable;

@Data
public class CommonResult {
    private @Nullable String exception;
    private @Nullable String message;
}
