package run.ikaros.resource;

import java.util.UUID;

/**
 * Resource 标签 API 视图。
 *
 * @param id 标签标识
 * @param name 标签名称
 * @param color 标签展示颜色
 */
public record ResourceTagView(UUID id, String name, String color) {
}
