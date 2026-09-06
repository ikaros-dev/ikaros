package run.ikaros.storage;

import java.util.UUID;

/** 可用于附件交付的 Provider 选项；选项本身不生成访问 URL。 */
public record AttachmentDeliveryProviderOptionView(UUID bindingId, UUID deliveryProviderId,
    String deliveryProviderKey, String displayName, DeliveryProviderType providerType,
    int priority, boolean selected) { }
