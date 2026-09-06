package run.ikaros.storage.api;

import java.util.UUID;

/** Stable Attachment reference without exposing Blob or Provider persistence types. */
public record AttachmentReference(UUID attachmentId, UUID resourceId) { }
