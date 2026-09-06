package run.ikaros.storage.api;

import java.util.UUID;

/** Business-facing Attachment view without Blob, Provider, or Placement internals. */
public record AttachmentView(UUID id, UUID resourceId, String fileName, AttachmentKind kind, String sha256,
                             long sizeBytes, String mediaType, AttachmentAvailabilityStatus availability) { }
