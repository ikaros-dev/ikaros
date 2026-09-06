package run.ikaros.relation;

import java.util.UUID;

/**
 * Resource 详情页可读取的关系摘要。
 */
public record ResourceRelationView(UUID id, UUID targetResourceId, ResourceRelationType type, int position) {
}
