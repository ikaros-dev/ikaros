package run.ikaros.resource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 为 Resource 绑定外部平台身份的请求数据。
 */
public record CreateExternalIdentityRequest(
    @NotBlank @Size(max = 128) String provider,
    @NotBlank @Size(max = 128) String type,
    @NotBlank @Size(max = 512) String value
) {
}
