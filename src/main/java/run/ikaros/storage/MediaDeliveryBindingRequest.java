package run.ikaros.storage;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MediaDeliveryBindingRequest(@NotBlank String deliveryProviderKey,
    @Min(0) int priority, boolean enabled, @NotNull DeliveryBindingCacheKeyPolicy cacheKeyPolicy,
    @NotNull DeliveryBindingRangePolicy rangePolicy, boolean fallbackParticipation) {}
