package run.ikaros.server.event;

import org.springframework.context.ApplicationEvent;

public class QbittorrentOptionUpdateEvent extends ApplicationEvent {
    public QbittorrentOptionUpdateEvent(Object source) {
        super(source);
    }
}
