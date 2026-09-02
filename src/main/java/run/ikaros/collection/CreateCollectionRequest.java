package run.ikaros.collection;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建逻辑集合的请求数据。
 */
public record CreateCollectionRequest(
    @NotBlank @Size(max = 256) String name,
    @Size(max = 2000) String description
) {
}
