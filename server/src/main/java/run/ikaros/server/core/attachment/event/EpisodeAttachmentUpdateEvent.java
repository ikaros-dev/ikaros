package run.ikaros.server.core.attachment.event;

import org.springframework.context.ApplicationEvent;

public class EpisodeAttachmentUpdateEvent extends ApplicationEvent {
    private final Long episodeId;
    private final Long attachmentId;
    private final Boolean notify;

    /**
     * Construct.
     */
    public EpisodeAttachmentUpdateEvent(Object source, Long episodeId,
                                        Long attachmentId, Boolean notify) {
        super(source);
        this.episodeId = episodeId;
        this.attachmentId = attachmentId;
        this.notify = notify;
    }

    public Long getEpisodeId() {
        return episodeId;
    }

    public Long getAttachmentId() {
        return attachmentId;
    }

    public Boolean getNotify() {
        return notify;
    }
}
