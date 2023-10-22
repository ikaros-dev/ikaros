package run.ikaros.server.core.user;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UpdateUserRequest {
    @Schema(requiredMode = REQUIRED)
    private String username;
    private String avatar;
    private String nickname;
    private String introduce;
    private String site;

}
