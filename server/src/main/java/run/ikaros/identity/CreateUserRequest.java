package run.ikaros.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建平台用户时接受的非敏感资料。
 */
public record CreateUserRequest(
    @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9_.-]*") String username,
    @NotBlank @Size(max = 128) String displayName,
    @Email @Size(max = 320) String email
) {
}
