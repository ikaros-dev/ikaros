package run.ikaros.resource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 向客户端输出的完整 Resource 视图。
 */
public record ResourceView(
    UUID id,
    ResourceType type,
    ResourceLifecycle lifecycle,
    List<ResourceTitleView> titles,
    List<ExternalIdentityView> externalIdentities,
    Instant createdAt,
    Instant updatedAt
) {
}
