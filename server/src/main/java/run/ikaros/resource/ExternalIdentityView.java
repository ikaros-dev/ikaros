package run.ikaros.resource;

import java.util.UUID;

/**
 * Resource 外部身份的 API 视图。
 */
public record ExternalIdentityView(UUID id, String provider, String type, String value) {
}
