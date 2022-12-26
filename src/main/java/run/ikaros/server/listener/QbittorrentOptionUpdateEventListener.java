package run.ikaros.server.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import run.ikaros.server.event.QbittorrentOptionUpdateEvent;
import run.ikaros.server.tripartite.qbittorrent.QbittorrentClient;

import jakarta.annotation.Nonnull;

@Slf4j
@Component
public class QbittorrentOptionUpdateEventListener implements
    ApplicationListener<QbittorrentOptionUpdateEvent> {

    private final QbittorrentClient qbittorrentClient;

    public QbittorrentOptionUpdateEventListener(QbittorrentClient qbittorrentClient) {
        this.qbittorrentClient = qbittorrentClient;
    }

    @Override
    public void onApplicationEvent(@Nonnull QbittorrentOptionUpdateEvent event) {
        log.debug("receive QbittorrentOptionUpdateEvent");
        this.qbittorrentClient.refreshHttpHeadersCookies();
    }
}
