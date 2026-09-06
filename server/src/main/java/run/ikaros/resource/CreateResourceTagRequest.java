package run.ikaros.resource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建用户自定义标签的请求。
 *
 * @param name 标签名称
 * @param color 可选展示颜色
 */
public record CreateResourceTagRequest(
    @NotBlank @Size(max = 128) String name,
    @Size(max = 32) String color
) {
}
