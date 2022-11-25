package run.ikaros.server.core.service;

import run.ikaros.server.model.dto.MetadataDTO;

import javax.annotation.Nonnull;
import java.util.List;

public interface MetadataService {
    @Nonnull
    List<MetadataDTO> search(@Nonnull String keyword);
}
