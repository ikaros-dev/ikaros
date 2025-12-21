package run.ikaros.server.core.attachment.service.impl;

import static run.ikaros.api.core.attachment.AttachmentConst.DRIVER_STATIC_RESOURCE_PREFIX;
import static run.ikaros.api.infra.utils.ReactiveBeanUtils.copyProperties;

import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentConst;
import run.ikaros.api.core.attachment.AttachmentDriver;
import run.ikaros.api.core.attachment.AttachmentDriverFetcher;
import run.ikaros.api.core.attachment.AttachmentSearchCondition;
import run.ikaros.api.core.attachment.exception.NoAvailableAttDriverFetcherException;
import run.ikaros.api.store.enums.AttachmentDriverType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.core.attachment.event.AttachmentDriverDisableEvent;
import run.ikaros.server.core.attachment.event.AttachmentDriverEnableEvent;
import run.ikaros.server.core.attachment.service.AttachmentDriverService;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.plugin.ExtensionComponentsFinder;
import run.ikaros.server.store.entity.AttachmentDriverEntity;
import run.ikaros.server.store.repository.AttachmentDriverRepository;
import run.ikaros.server.store.repository.AttachmentRepository;

@Slf4j
@Service
public class AttachmentDriverServiceImpl implements AttachmentDriverService {
    private final AttachmentDriverRepository repository;
    private final AttachmentRepository attachmentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AttachmentService attachmentService;

    private final R2dbcEntityTemplate template;
    private final ExtensionComponentsFinder extensionComponentsFinder;

    /**
     * .
     */
    public AttachmentDriverServiceImpl(AttachmentDriverRepository repository,
                                       AttachmentRepository attachmentRepository,
                                       ApplicationEventPublisher eventPublisher,
                                       AttachmentService attachmentService,
                                       R2dbcEntityTemplate template,
                                       ExtensionComponentsFinder extensionComponentsFinder) {
        this.repository = repository;
        this.attachmentRepository = attachmentRepository;
        this.eventPublisher = eventPublisher;
        this.attachmentService = attachmentService;
        this.template = template;
        this.extensionComponentsFinder = extensionComponentsFinder;
    }

    private AttachmentDriverFetcher getAttDriverFetcher(
        AttachmentDriverType type, String driverName
    ) {
        Assert.notNull(type, "'type' must not be null.");
        Assert.hasText(driverName, "'driverName' must has text.");
        return extensionComponentsFinder.getExtensions(AttachmentDriverFetcher.class)
            .stream()
            .filter(fetcher -> type.equals(fetcher.getDriverType()))
            .filter(fetcher -> driverName.equals(fetcher.getDriverName()))
            .findFirst()
            .orElseThrow(() -> new NoAvailableAttDriverFetcherException(
                "No found available attachment driver fetcher for type: "
                    + type.name() + " driverName: " + driverName
            ));
    }

    @Override
    public Mono<AttachmentDriver> save(AttachmentDriver driver) {
        Assert.notNull(driver, "'driver' must not null.");
        Assert.notNull(driver.getType(), "'driver type' must not null.");
        AttachmentDriverFetcher attDriverFetcher =
            getAttDriverFetcher(driver.getType(), driver.getName());
        return repository.findByTypeAndNameAndMountName(
                driver.getType().toString(), driver.getName(), driver.getMountName())
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
        return repository.findById(id)
            .map(entity -> {
                eventPublisher.publishEvent(new AttachmentDriverDisableEvent(this, entity));
                return entity.getId();
            })
            .flatMap(repository::deleteById);
    }

    @Override
    public Mono<Void> removeByTypeAndName(String type, String name) {
        Assert.notNull(type, "'type' must not null.");
        name = name.trim();
        return repository.findByTypeAndName(type, name)
            .map(entity -> {
                eventPublisher.publishEvent(new AttachmentDriverDisableEvent(this, entity));
                return entity.getId();
            })
            .flatMap(repository::deleteById);
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
    public Mono<PagingWrap<Attachment>> listAttachmentsByCondition(
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
        return attachmentService.findById(attachmentId)
            .filter(attachment ->
                attachment.getType() != null
                    && attachment.getType().name().toUpperCase(Locale.ROOT).startsWith("DRIVER_"))
            .flatMap(this::refreshRemoteFileSystem);
    }

    @Override
    public Mono<PagingWrap<AttachmentDriver>> listDriversByCondition(Integer page,
                                                                     Integer pageSize) {
        if (page == null || page <= 0) {
            page = 1;
        }
        if (pageSize == null || pageSize <= 0) {
            pageSize = 10;
        }
        final PageRequest pageRequest = PageRequest.of(page - 1, pageSize);

        Integer finalPage = page;
        Integer finalPageSize = pageSize;

        Query query = Query.query(Criteria.empty())
            .sort(Sort.by(Sort.Order.desc("d_order")))
            .with(pageRequest);

        return template.select(query, AttachmentDriverEntity.class)
            .flatMap(entity -> copyProperties(entity, new AttachmentDriver()))
            .collectList()
            .flatMap(attachments -> template.count(query, AttachmentDriverEntity.class)
                .map(total -> new PagingWrap<>(finalPage, finalPageSize, total, attachments)));
    }

    private Mono<Void> refreshRemoteFileSystem(Attachment attachment) {
        final Long pid = attachment.getId();
        String url = attachment.getUrl();
        Long driverId = attachment.getDriverId();
        String remotePath = attachment.getFsPath();
        return repository.findById(driverId)
            .flatMap(entity -> copyProperties(entity, new AttachmentDriver()))
            .flatMapMany(attachmentDriverEntity ->
                fetchAndUpdateEntities(attachmentDriverEntity, pid, remotePath))
            .then();
    }

    private Flux<Attachment> fetchAndUpdateEntities(
        AttachmentDriver driver, Long pid, String remotePath) {
        AttachmentDriverType type = driver.getType();
        AttachmentDriverFetcher driverFetcher = getAttDriverFetcher(type, driver.getName());
        List<Attachment> attachments = driverFetcher.getChildren(driver.getId(), pid, remotePath);

        return Flux.fromStream(attachments.stream())
            .flatMap(attachmentService::save)
            .map(att -> att.setUrl(DRIVER_STATIC_RESOURCE_PREFIX + att.getPath()))
            .flatMap(attachmentService::save);
    }

}
