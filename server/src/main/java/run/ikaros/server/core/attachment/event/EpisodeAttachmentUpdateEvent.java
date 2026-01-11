package run.ikaros.server.core.attachment.event;

import java.util.UUID;
import org.springframework.context.ApplicationEvent;

public class EpisodeAttachmentUpdateEvent extends ApplicationEvent {
    private final UUID episodeId;
    private final UUID attachmentId;
    private final Boolean notify;

    /**
     * Construct.
     */
    public EpisodeAttachmentUpdateEvent(Object source, UUID episodeId,
                                        UUID attachmentId, Boolean notify) {
        super(source);
        this.episodeId = episodeId;
        this.attachmentId = attachmentId;
        this.notify = notify;
    }

    public UUID getEpisodeId() {
        return episodeId;
    }

    public UUID getAttachmentId() {
        return attachmentId;
    }

    public Boolean getNotify() {
        return notify;
    }
}
