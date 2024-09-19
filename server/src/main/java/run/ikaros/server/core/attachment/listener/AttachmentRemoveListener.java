package run.ikaros.server.core.attachment.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.server.core.attachment.event.AttachmentRemoveEvent;
import run.ikaros.server.store.repository.AttachmentReferenceRepository;
import run.ikaros.server.store.repository.AttachmentRelationRepository;

@Slf4j
@Component
public class AttachmentRemoveListener {
    private final AttachmentRelationRepository relationRepository;
    private final AttachmentReferenceRepository referenceRepository;

    public AttachmentRemoveListener(AttachmentRelationRepository relationRepository,
                                    AttachmentReferenceRepository referenceRepository) {
        this.relationRepository = relationRepository;
        this.referenceRepository = referenceRepository;
    }

    /**
     * 清除存在的附件关系.
     */
    @EventListener(AttachmentRemoveEvent.class)
    public Mono<Void> onAttachmentRemoveEvent(AttachmentRemoveEvent event) {
        Long attId = event.getEntity().getId();
        return relationRepository.deleteAllByAttachmentId(attId)
            .then(referenceRepository.deleteAllByAttachmentId(attId));
    }
}
