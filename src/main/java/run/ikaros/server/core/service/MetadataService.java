package run.ikaros.server.core.service;

import run.ikaros.server.model.response.MetadataSearchResponse;

import javax.annotation.Nonnull;
import java.util.List;

public interface MetadataService {
    @Nonnull
    List<MetadataSearchResponse> search(@Nonnull String keyword);
}
