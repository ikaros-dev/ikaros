package run.ikaros.storage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record DeliveryProviderWriteRequest(@NotBlank String providerKey, @NotNull DeliveryProviderType providerType,
    @NotBlank String displayName, String credentialRef, Map<String, Object> config, Boolean enabled) {}
