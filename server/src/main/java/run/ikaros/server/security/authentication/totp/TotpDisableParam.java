package run.ikaros.server.security.authentication.totp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;

/**
 * TOTP 禁用请求参数，承载需要校验的当前登录密码.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class TotpDisableParam {
    /** 当前登录密码，用于确认禁用操作由账号本人发起. */
    @Schema(description = "当前登录密码")
    private @Nullable String password;
}
