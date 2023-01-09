package run.ikaros.server.custom;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import org.springframework.util.Assert;
import run.ikaros.server.store.entity.CustomEntity;
import run.ikaros.server.store.entity.CustomMetadataEntity;

public record CustomDto(@Nonnull CustomEntity customEntity,
                        @Nullable List<CustomMetadataEntity> customMetadataEntityList) {

    CustomDto updateMetadataCustomId() {
        Long customId = customEntity.getId();
        Assert.notNull(customId, "custom id must not null");
        customMetadataEntityList.forEach(
            customMetadataEntity -> customMetadataEntity.setCustomId(customId));
        return this;
    }
}
