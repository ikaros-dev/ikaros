package run.ikaros.server.core.subject.event;

import org.springframework.context.ApplicationEvent;

public class EpisodeFileUpdateEvent extends ApplicationEvent {
    private final Long episodeId;
    private final Long fileId;
    private final Boolean notify;

    /**
     * Construct.
     */
    public EpisodeFileUpdateEvent(Object source, Long episodeId, Long fileId, Boolean notify) {
        super(source);
        this.episodeId = episodeId;
        this.fileId = fileId;
        this.notify = notify;
    }

    public Long getEpisodeId() {
        return episodeId;
    }

    public Long getFileId() {
        return fileId;
    }

    public Boolean getNotify() {
        return notify;
    }
}
