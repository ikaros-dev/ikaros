package run.ikaros.server.service;

import org.springframework.stereotype.Service;
import run.ikaros.server.core.service.TorrentService;
import run.ikaros.server.enums.TorrentType;
import run.ikaros.server.tripartite.qbittorrent.QbittorrentClient;
import run.ikaros.server.utils.AssertUtils;

@Service
public class TorrentServiceImpl implements TorrentService {

    private final QbittorrentClient qbittorrentClient;

    public TorrentServiceImpl(QbittorrentClient qbittorrentClient) {
        this.qbittorrentClient = qbittorrentClient;
    }

    @Override
    public Boolean create(TorrentType type, String torrentUrl) {
        AssertUtils.notNull(type, "type");
        AssertUtils.notBlank(torrentUrl, "torrentUrl");

        qbittorrentClient.addTorrentFromUrl(torrentUrl);

        return Boolean.TRUE;
    }
}
