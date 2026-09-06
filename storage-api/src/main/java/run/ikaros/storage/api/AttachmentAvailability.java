package run.ikaros.storage.api;

import java.util.UUID;

/** Stable Storage availability result; Provider and Placement details stay private. */
public record AttachmentAvailability(UUID attachmentId, AttachmentAvailabilityStatus status) { }
