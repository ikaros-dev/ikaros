package run.ikaros.server.core.episode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.server.store.entity.EpisodeEntity;
import run.ikaros.server.store.repository.AttachmentReferenceRepository;

@Slf4j
@Component
public class EpisodeRemoveEventListener {
    private final AttachmentReferenceRepository attachmentReferenceRepository;

    public EpisodeRemoveEventListener(AttachmentReferenceRepository attachmentReferenceRepository) {
        this.attachmentReferenceRepository = attachmentReferenceRepository;
    }

    /**
     * 移除所有的剧集绑定的对应的附件引用.
     */
    @EventListener(EpisodeRemoveEvent.class)
    public Mono<Void> onEpisodeRemoveEvent(EpisodeRemoveEvent event) {
        EpisodeEntity entity = event.getEntity();
        final Long episodeId = entity.getId();
        return attachmentReferenceRepository.deleteAllByTypeAndReferenceId(
            AttachmentReferenceType.EPISODE,
            episodeId
        ).doOnSuccess(v -> log.debug("remove all attachment refs for episode: {}", entity));
    }

}
