package run.ikaros.server.core.attachment.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.server.core.attachment.event.AttachmentRemoveEvent;
import run.ikaros.server.store.repository.AttachmentRelationRepository;

@Slf4j
@Component
public class AttachmentRemoveListener {
    private final AttachmentRelationRepository relationRepository;

    public AttachmentRemoveListener(AttachmentRelationRepository relationRepository) {
        this.relationRepository = relationRepository;
    }

    /**
     * 清除存在附件关系.
     */
    @EventListener(AttachmentRemoveEvent.class)
    public Mono<Void> onAttachmentRemoveEvent(AttachmentRemoveEvent event) {
        Long attId = event.getEntity().getId();
        return relationRepository.findAllByAttachmentId(attId)
            .flatMap(relationRepository::delete)
            .then();
    }
}
