package run.ikaros.resource.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Collection 的 API 视图。
 */
public record CollectionView(UUID id, UUID parentId, String name, String description,
                             Instant createdAt, Instant updatedAt, Long version) {
    public CollectionView(UUID id, String name, String description, Instant createdAt, Instant updatedAt) {
        this(id, null, name, description, createdAt, updatedAt, null);
    }

    public CollectionView(UUID id, UUID parentId, String name, String description,
                          Instant createdAt, Instant updatedAt) {
        this(id, parentId, name, description, createdAt, updatedAt, null);
    }
}
