package run.ikaros.verification;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;

/**
 * 验证一次性验证码时接受的用户输入。
 */
public record VerifyOtpRequest(@NotBlank @Pattern(regexp = "\\d{6}") String code) {
}
