package run.ikaros.server.core.service;

import javax.annotation.Nonnull;

public interface TorrentService {
    void create(@Nonnull String torrentUrl);
}
