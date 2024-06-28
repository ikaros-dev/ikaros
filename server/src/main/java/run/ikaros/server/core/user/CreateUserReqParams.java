package run.ikaros.server.core.user;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CreateUserReqParams {
    @Schema(requiredMode = REQUIRED)
    private String username;
    private String password;
    private Boolean enabled;
}
