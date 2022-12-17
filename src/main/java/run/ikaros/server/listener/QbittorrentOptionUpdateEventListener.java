package run.ikaros.server.listener;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import run.ikaros.server.event.QbittorrentOptionUpdateEvent;
import run.ikaros.server.tripartite.qbittorrent.QbittorrentClient;

import jakarta.annotation.Nonnull;

@Component
public class QbittorrentOptionUpdateEventListener implements
    ApplicationListener<QbittorrentOptionUpdateEvent> {

    private final QbittorrentClient qbittorrentClient;

    public QbittorrentOptionUpdateEventListener(QbittorrentClient qbittorrentClient) {
        this.qbittorrentClient = qbittorrentClient;
    }

    @Override
    public void onApplicationEvent(@Nonnull QbittorrentOptionUpdateEvent event) {
        this.qbittorrentClient.refreshHttpHeadersCookies();
    }
}
