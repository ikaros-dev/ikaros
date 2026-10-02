package run.ikaros.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import run.ikaros.storage.api.DeliveryProviderType;

class AttachmentPreviewUrlSerializationTest {
    @Test
    void serializesPreviewAndProviderOptionsWithSnakeCaseProperties() {
        UUID bindingId = UUID.randomUUID();
        UUID providerId = UUID.randomUUID();
        AttachmentDeliveryProviderOptionView provider = new AttachmentDeliveryProviderOptionView(
            bindingId, providerId, "cdn", "Media CDN", DeliveryProviderType.CDN, 10, true);
        AttachmentPreviewUrlView view = new AttachmentPreviewUrlView("GET", "https://media.example.com/video.mp4",
            Instant.parse("2026-10-02T08:00:00Z"), true, "video/mp4", provider, List.of(provider));

        JsonNode json = new ObjectMapper().valueToTree(view);

        assertThat(json.get("expires_at").asText()).isEqualTo("2026-10-02T08:00:00Z");
        assertThat(json.get("range_supported").asBoolean()).isTrue();
        assertThat(json.get("content_type").asText()).isEqualTo("video/mp4");
        assertThat(json.has("expiresAt")).isFalse();
        assertThat(json.has("rangeSupported")).isFalse();
        assertThat(json.has("contentType")).isFalse();
        assertThat(json.has("selectedProvider")).isFalse();
        JsonNode selected = json.get("selected_provider");
        assertThat(selected.get("binding_id").asText()).isEqualTo(bindingId.toString());
        assertThat(selected.get("delivery_provider_id").asText()).isEqualTo(providerId.toString());
        assertThat(selected.get("delivery_provider_key").asText()).isEqualTo("cdn");
        assertThat(selected.get("display_name").asText()).isEqualTo("Media CDN");
        assertThat(selected.get("provider_type").asText()).isEqualTo("CDN");
        for (String property : List.of("bindingId", "deliveryProviderId", "deliveryProviderKey", "displayName",
            "providerType")) {
            assertThat(selected.has(property)).isFalse();
        }
        assertThat(json.get("providers").get(0)).isEqualTo(selected);
    }
}
