package run.ikaros.server.security.authentication.totp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * TOTP状态响应.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class TotpStatusResponse {
    @Schema(description = "是否已启用二步验证")
    private Boolean enabled;
}
