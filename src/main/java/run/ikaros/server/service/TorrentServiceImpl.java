package run.ikaros.server.service;

import org.springframework.stereotype.Service;
import run.ikaros.server.core.service.TorrentService;
import run.ikaros.server.tripartite.qbittorrent.QbittorrentClient;
import run.ikaros.server.utils.AssertUtils;

import javax.annotation.Nonnull;

@Service
public class TorrentServiceImpl implements TorrentService {

    private final QbittorrentClient qbittorrentClient;

    public TorrentServiceImpl(QbittorrentClient qbittorrentClient) {
        this.qbittorrentClient = qbittorrentClient;
    }

    @Override
    public void create(@Nonnull String torrentUrl) {
        AssertUtils.notBlank(torrentUrl, "torrentUrl");
        qbittorrentClient.addTorrentFromUrl(torrentUrl);
    }
}
