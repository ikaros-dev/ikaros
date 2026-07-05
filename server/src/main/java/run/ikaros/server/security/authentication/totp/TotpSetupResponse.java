package run.ikaros.server.security.authentication.totp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * TOTP设置响应.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class TotpSetupResponse {
    @Schema(description = "TOTP密钥(Base32)")
    private String secret;
    @Schema(description = "otpauth URI，用于生成二维码")
    private String otpAuthUri;
}
