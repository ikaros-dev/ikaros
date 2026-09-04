package run.ikaros.storage;

import java.time.Instant;

public record AttachmentPreviewUrlView(String method, String url, Instant expiresAt,
                                       boolean rangeSupported, String contentType) { }
