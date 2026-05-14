package run.ikaros.server.security.authentication.jwt;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class JwtApplyParam {
    @Schema(requiredMode = REQUIRED, description = "认证类型", implementation = Type.class)
    private Type authType;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "密码")
    private String password;
    @Schema(description = "手机号")
    private String phoneNum;
    @Schema(description = "邮箱")
    private String email;
    @Schema(description = "验证码")
    private String code;

    public enum Type {
        /**
         * 用户名密码认证.
         */
        USERNAME_PASSWORD,
        /**
         * 手机号密码认证.
         */
        PHONE_NUMBER_PASSWORD,
        /**
         * 手机号短信验证码认证.
         */
        PHONE_NUMBER_CODE,
        /**
         * 邮箱密码认证.
         */
        EMAIL_PASSWORD,
        /**
         * 邮箱和邮件验证码认证.
         */
        EMAIL_CODE,
    }
}


