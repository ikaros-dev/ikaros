package run.ikaros.authentication;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建平台用户时接受的资料与初始密码。
 */
public record CreateUserRequest(
    @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9_.-]*") String username,
    @NotBlank @Size(max = 128) String displayName,
    @Email @Size(max = 320) String email,
    @NotBlank @Size(min = 8, max = 128) String password
) {
}
