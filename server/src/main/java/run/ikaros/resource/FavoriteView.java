package run.ikaros.resource;

import java.util.UUID;

/**
 * Resource 收藏状态视图。
 *
 * @param resourceId Resource 标识
 * @param favorite 当前用户是否已收藏
 */
public record FavoriteView(UUID resourceId, boolean favorite) {
}
