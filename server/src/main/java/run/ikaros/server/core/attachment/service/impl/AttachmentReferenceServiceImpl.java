package run.ikaros.server.core.attachment.service.impl;

import static run.ikaros.server.infra.utils.ReactiveBeanUtils.copyProperties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.AttachmentReference;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.server.core.attachment.service.AttachmentReferenceService;
import run.ikaros.server.store.entity.AttachmentReferenceEntity;
import run.ikaros.server.store.repository.AttachmentReferenceRepository;

@Slf4j
@Service
public class AttachmentReferenceServiceImpl implements AttachmentReferenceService {
    private final AttachmentReferenceRepository repository;

    public AttachmentReferenceServiceImpl(AttachmentReferenceRepository repository) {
        this.repository = repository;
    }


    @Override
    public Mono<AttachmentReference> save(AttachmentReference attachmentReference) {
        Assert.notNull(attachmentReference, "'attachmentReference' must not null.");
        return copyProperties(attachmentReference, new AttachmentReferenceEntity())
            .flatMap(repository::save)
            .flatMap(entity -> copyProperties(entity, attachmentReference));
    }

    @Override
    public Flux<AttachmentReference> findAllByTypeAndAttachmentId(AttachmentReferenceType type,
                                                                  Long attachmentId) {
        Assert.notNull(type, "'type' must not null.");
        Assert.isTrue(attachmentId > 0, "'attachmentId' must > 0.");
        return repository.findAllByTypeAndAttachmentId(type, attachmentId)
            .flatMap(entity -> copyProperties(entity, new AttachmentReference()));
    }

    @Override
    public Mono<Void> removeById(Long attachmentRefId) {
        return repository.deleteById(attachmentRefId);
    }
}
