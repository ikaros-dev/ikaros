package run.ikaros.server.security.authentication.totp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * TOTP验证请求参数.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class TotpValidateParam {
    @Schema(description = "临时令牌(来自applyJwtToken返回)")
    private String tempToken;
    @Schema(description = "TOTP验证码(6位数字)")
    private String code;
}
