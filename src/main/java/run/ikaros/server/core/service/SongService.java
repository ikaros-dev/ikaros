package run.ikaros.server.core.service;

import run.ikaros.server.entity.SongEntity;
import run.ikaros.server.model.dto.SongDTO;
import run.ikaros.server.model.request.SearchSongRequest;
import run.ikaros.server.result.PagingWrap;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public interface SongService extends CrudService<SongEntity, Long> {
    @Nonnull
    PagingWrap<SongDTO> findSongs(@Nullable SearchSongRequest searchSongRequest);
}
