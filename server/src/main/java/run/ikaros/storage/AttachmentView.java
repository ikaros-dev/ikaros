package run.ikaros.storage;

import java.util.List;
import java.util.UUID;

/**
 * Attachment、Blob 和已知 Placement 的组合 API 视图。
 */
public record AttachmentView(UUID id, String fileName, AttachmentKind kind, UUID blobId, String sha256,
                             long sizeBytes, String mediaType, BlobAvailability availability,
                             List<PlacementView> placements) {
}
