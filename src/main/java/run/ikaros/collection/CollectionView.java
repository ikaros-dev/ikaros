package run.ikaros.collection;

import java.time.Instant;
import java.util.UUID;

/**
 * Collection 的 API 视图。
 */
public record CollectionView(UUID id, String name, String description, Instant createdAt, Instant updatedAt) {
}
