package run.ikaros.server.core.attachment.service.impl;

import static run.ikaros.api.infra.utils.ReactiveBeanUtils.copyProperties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.AttachmentDriver;
import run.ikaros.api.core.attachment.exception.AttachmentDriverRemoveException;
import run.ikaros.server.core.attachment.event.AttachmentDriverDisableEvent;
import run.ikaros.server.core.attachment.event.AttachmentDriverEnableEvent;
import run.ikaros.server.core.attachment.service.AttachmentDriverService;
import run.ikaros.server.store.entity.AttachmentDriverEntity;
import run.ikaros.server.store.repository.AttachmentDriverRepository;

@Slf4j
@Service
public class AttachmentDriverServiceImpl implements AttachmentDriverService {
    private final AttachmentDriverRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public AttachmentDriverServiceImpl(AttachmentDriverRepository repository,
                                       ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
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

    @Override
    public Mono<Void> removeById(Long id) {
        Assert.notNull(id, "'id' must not null.");
        return repository.deleteById(id);
    }

    @Override
    public Mono<Void> removeByTypeAndName(String type, String name) {
        Assert.notNull(type, "'type' must not null.");
        name = name.trim();
        return repository.deleteByTypeAndName(type, name)
            .filter(count -> count > 0)
            .switchIfEmpty(Mono.error(new AttachmentDriverRemoveException(
                "Remove attachment driver fail for type=" + type + " and name=" + name)))
            .then();
    }

    @Override
    public Mono<AttachmentDriver> findById(Long id) {
        Assert.notNull(id, "'id' must not null.");
        return repository.findById(id)
            .flatMap(entity -> copyProperties(entity, new AttachmentDriver()));
    }

    @Override
    public Mono<AttachmentDriver> findByTypeAndName(String type, String name) {
        Assert.notNull(type, "'type' must not null.");
        name = name.trim();
        return repository.findByTypeAndName(type, name)
            .flatMap(entity -> copyProperties(entity, new AttachmentDriver()));
    }

    @Override
    public Mono<Void> enable(Long driverId) {
        Assert.notNull(driverId, "'driverId' must not null.");
        return repository.findById(driverId)
            .map(entity -> entity.setEnable(true))
            .flatMap(repository::save)
            .doOnSuccess(entity ->
                eventPublisher.publishEvent(new AttachmentDriverEnableEvent(this, entity)))
            .then();
    }

    @Override
    public Mono<Void> disable(Long driverId) {
        Assert.notNull(driverId, "'driverId' must not null.");
        return repository.findById(driverId)
            .map(entity -> entity.setEnable(false))
            .flatMap(repository::save)
            .doOnSuccess(entity ->
                eventPublisher.publishEvent(new AttachmentDriverDisableEvent(this, entity)))
            .then();
    }


}
