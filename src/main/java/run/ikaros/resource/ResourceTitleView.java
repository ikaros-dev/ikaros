package run.ikaros.resource;

import java.util.UUID;

/**
 * Resource 标题的 API 视图。
 */
public record ResourceTitleView(UUID id, String locale, String value, boolean primary) {
}
