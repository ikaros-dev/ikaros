package run.ikaros.server.core.user;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jspecify.annotations.Nullable;

@Data
public class CreateUserReqParams {
    @Schema(requiredMode = REQUIRED)
    private @Nullable String username;
    private @Nullable String password;
    private @Nullable Boolean enabled;
}
