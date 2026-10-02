package run.ikaros.storage;

import run.ikaros.storage.api.*;

import java.util.UUID;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/** 可用于附件交付的 Provider 选项；选项本身不生成访问 URL。 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AttachmentDeliveryProviderOptionView(UUID bindingId, UUID deliveryProviderId,
    String deliveryProviderKey, String displayName, DeliveryProviderType providerType,
    int priority, boolean selected) { }
