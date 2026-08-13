package run.ikaros.server.custom;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import run.ikaros.server.store.entity.CustomEntity;
import run.ikaros.server.store.entity.CustomMetadataEntity;

public record CustomDto(CustomEntity customEntity,
                        @Nullable List<CustomMetadataEntity> customMetadataEntityList) {

    CustomDto updateMetadataCustomId() {
        UUID customId = customEntity.getId();
        Assert.notNull(customId, "custom id must not null");
        Objects.requireNonNull(customMetadataEntityList).forEach(
            customMetadataEntity -> customMetadataEntity.setCustomId(customId));
        return this;
    }
}
