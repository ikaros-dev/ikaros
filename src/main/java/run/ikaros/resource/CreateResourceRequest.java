package run.ikaros.resource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建 Resource 的请求数据。首个标题必须在创建时一并提供。
 */
public record CreateResourceRequest(
    @NotNull ResourceType type,
    @NotBlank @Size(max = 512) String title,
    @NotBlank @Size(max = 32) String locale
) {
}
