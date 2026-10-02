package run.ikaros.storage.api;

import java.util.UUID;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/** Metadata exposed by the cross-owner attachment management query. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AdminAttachmentItem(UUID id, UUID resourceId, String fileName, AttachmentKind kind,
                                  String sha256, long sizeBytes, String mediaType,
                                  AttachmentAvailabilityStatus availability) { }
