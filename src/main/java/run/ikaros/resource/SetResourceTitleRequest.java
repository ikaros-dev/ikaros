package run.ikaros.resource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新增或修改 Resource 本地化标题的请求。
 *
 * @param locale 标题语言或地区代码
 * @param title 标题内容
 * @param primary 是否将该标题设为主标题
 */
public record SetResourceTitleRequest(
    @NotBlank @Size(max = 32) String locale,
    @NotBlank @Size(max = 512) String title,
    boolean primary
) {
}
