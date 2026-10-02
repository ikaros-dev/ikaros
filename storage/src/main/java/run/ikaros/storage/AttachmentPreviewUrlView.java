package run.ikaros.storage;

import java.time.Instant;
import java.util.List;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AttachmentPreviewUrlView(String method, String url, Instant expiresAt,
                                       boolean rangeSupported, String contentType,
                                       AttachmentDeliveryProviderOptionView selectedProvider,
                                       List<AttachmentDeliveryProviderOptionView> providers) {
    public AttachmentPreviewUrlView(String method, String url, Instant expiresAt,
        boolean rangeSupported, String contentType) {
        this(method, url, expiresAt, rangeSupported, contentType, null, List.of());
    }
}
