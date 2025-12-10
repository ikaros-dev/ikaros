package run.ikaros.server.core.attachment.service.impl;

import static run.ikaros.api.infra.utils.ReactiveBeanUtils.copyProperties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.AttachmentDriver;
import run.ikaros.server.core.attachment.service.AttachmentDriverService;
import run.ikaros.server.store.entity.AttachmentDriverEntity;
import run.ikaros.server.store.repository.AttachmentDriverRepository;

@Slf4j
@Service
public class AttachmentDriverServiceImpl implements AttachmentDriverService {
    private final AttachmentDriverRepository repository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public AttachmentDriverServiceImpl(AttachmentDriverRepository repository,
                                       ApplicationEventPublisher applicationEventPublisher) {
        this.repository = repository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public Mono<AttachmentDriver> save(AttachmentDriver driver) {
        Assert.notNull(driver, "'driver' must not null.");
        Assert.notNull(driver.getType(), "'driver type' must not null.");
        return repository.findByTypeAndName(driver.getType().toString(), driver.getName())
            .switchIfEmpty(Mono.defer(() -> copyProperties(driver, new AttachmentDriverEntity())
                    .flatMap(repository::save))
                .doOnSuccess(entity ->
                    log.debug("Created attachment driver with type={} and name={}",
                        entity.getType(), entity.getName())))
            .flatMap(entity -> copyProperties(entity, new AttachmentDriver()));
    }
}
