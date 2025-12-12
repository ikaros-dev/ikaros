package run.ikaros.server.core.attachment.service.impl;

import static run.ikaros.api.core.attachment.AttachmentConst.DRIVER_URL_SPLIT_STR;
import static run.ikaros.api.infra.utils.ReactiveBeanUtils.copyProperties;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentConst;
import run.ikaros.api.core.attachment.AttachmentDriver;
import run.ikaros.api.core.attachment.AttachmentSearchCondition;
import run.ikaros.api.core.attachment.exception.AttachmentDriverRemoveException;
import run.ikaros.api.store.enums.AttachmentDriverType;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.core.attachment.event.AttachmentDriverDisableEvent;
import run.ikaros.server.core.attachment.event.AttachmentDriverEnableEvent;
import run.ikaros.server.core.attachment.service.AttachmentDriverService;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.store.entity.AttachmentDriverEntity;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.repository.AttachmentDriverRepository;

@Slf4j
@Service
public class AttachmentDriverServiceImpl implements AttachmentDriverService {
    private final AttachmentDriverRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final AttachmentService attachmentService;

    /**
     * .
     */
    public AttachmentDriverServiceImpl(AttachmentDriverRepository repository,
                                       ApplicationEventPublisher eventPublisher,
                                       AttachmentService attachmentService) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.attachmentService = attachmentService;
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
            .flatMap(entity -> copyProperties(driver, entity, "id"))
            .flatMap(repository::save)
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

    @Override
    public Mono<PagingWrap<Attachment>> listEntitiesByCondition(
        AttachmentSearchCondition attachmentSearchCondition) {
        Assert.notNull(attachmentSearchCondition, "'attachmentSearchCondition' must not null.");
        Boolean refresh = attachmentSearchCondition.getRefresh();
        Assert.notNull(refresh, "'refresh' must not null.");
        Long parentId = attachmentSearchCondition.getParentId();
        if (parentId == null) {
            parentId = AttachmentConst.ROOT_DIRECTORY_ID;
        }
        if (refresh) {
            return refresh(parentId)
                .then(attachmentService.listByCondition(attachmentSearchCondition));
        }
        return attachmentService.listByCondition(attachmentSearchCondition);
    }

    @Override
    public Mono<Void> refresh(Long attachmentId) {
        Assert.notNull(attachmentId, "'attachmentId' must not null.");
        return attachmentService.findEntityById(attachmentId)
            .filter(attachment ->
                attachment.getType() != null
                    && attachment.getType().name().toUpperCase(Locale.ROOT).startsWith("DRIVER_"))
            .flatMap(this::refreshRemoteFileSystem);
    }

    private Mono<Void> refreshRemoteFileSystem(AttachmentEntity attachment) {
        final Long pid = attachment.getId();
        String url = attachment.getUrl();
        Long driverId = Long.parseLong(url.substring(0, url.indexOf(DRIVER_URL_SPLIT_STR)));
        String remotePath = url.substring(url.indexOf(DRIVER_URL_SPLIT_STR) + 1);
        return repository.findById(driverId)
            .flatMapMany(attachmentDriverEntity ->
                fetchAndUpdateEntities(attachmentDriverEntity, pid, remotePath))
            .then();
    }

    private Flux<AttachmentEntity> fetchAndUpdateEntities(
        AttachmentDriverEntity driver, Long pid, String remotePath) {
        AttachmentDriverType type = driver.getType();
        switch (type) {
            case LOCAL -> {
                return fetchAndUpdateEntitiesWithTypeIsLocal(driver, pid, remotePath);
            }
            case WEBDAV -> {
                return fetchAndUpdateEntitiesWithTypeIsWebdav(driver, pid, remotePath);
            }
            case CUSTOM -> {
                return fetchAndUpdateEntitiesWithTypeIsCustom(driver, pid, remotePath);
            }
            default -> {
                return Flux.empty();
            }
        }
    }

    private Flux<AttachmentEntity> fetchAndUpdateEntitiesWithTypeIsLocal(
        AttachmentDriverEntity driver, Long pid, String remotePath) {
        File file = new File(remotePath);
        File[] files = file.listFiles();
        if (files == null) {
            return Flux.empty();
        }


        return Flux.fromArray(files)
            .map(f -> AttachmentEntity.builder()
                .parentId(pid)
                .type(f.isFile() ? AttachmentType.Driver_File : AttachmentType.Driver_Directory)
                .name(f.getName())
                .url(driver.getId() + DRIVER_URL_SPLIT_STR + f.getAbsolutePath())
                .path(f.getPath())
                .fsPath(f.getAbsolutePath())
                .size(f.isFile() ? file.length() : 0)
                .updateTime(LocalDateTime.now())
                .deleted(false)
                .build())
            .flatMap(attachmentService::saveEntity);
    }

    private Flux<AttachmentEntity> fetchAndUpdateEntitiesWithTypeIsWebdav(
        AttachmentDriverEntity driver, Long pid, String remotePath) {
        // todo impl webdav fs fetch
        return Flux.empty();
    }

    private Flux<AttachmentEntity> fetchAndUpdateEntitiesWithTypeIsCustom(
        AttachmentDriverEntity driver, Long pid, String remotePath) {
        // todo impl custom fs fetch
        return Flux.empty();
    }

}
