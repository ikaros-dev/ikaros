package run.ikaros.server.core.service;

import run.ikaros.server.enums.TorrentType;

public interface TorrentService {
    Boolean create(TorrentType type, String torrentUrl);
}
