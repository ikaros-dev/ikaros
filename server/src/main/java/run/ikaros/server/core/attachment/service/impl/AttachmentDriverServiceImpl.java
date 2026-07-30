package run.ikaros.server.core.attachment.service.impl;

import static run.ikaros.api.infra.utils.ReactiveBeanUtils.copyProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentConst;
import run.ikaros.api.core.attachment.AttachmentDriver;
import run.ikaros.api.core.attachment.AttachmentDriverFetcher;
import run.ikaros.api.core.attachment.AttachmentSearchCondition;
import run.ikaros.api.core.attachment.exception.AttachmentNotFoundException;
import run.ikaros.api.core.attachment.exception.NoAvailableAttDriverFetcherException;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AttachmentDriverType;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.core.attachment.event.AttachmentDriverDisableEvent;
import run.ikaros.server.core.attachment.event.AttachmentDriverEnableEvent;
import run.ikaros.server.core.attachment.service.AttachmentDriverService;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.core.attachment.vo.AttachmentDriverFetcherVo;
import run.ikaros.server.plugin.ExtensionComponentsFinder;
import run.ikaros.server.store.entity.AttachmentDriverEntity;
import run.ikaros.server.store.entity.AttachmentEntity;
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
    /** 正在执行的目录刷新任务，用于合并同一目录的并发请求. */
    private final ConcurrentMap<UUID, Mono<Void>> refreshTasks = new ConcurrentHashMap<>();

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
                    .map(entity -> {
                        if (entity.getId() == null) {
                            entity.setId(UuidV7Utils.generateUuid());
                        }
                        return entity;
                    })
                    .flatMap(repository::insert))
                .doOnSuccess(entity ->
                    log.debug("Created attachment driver with type={} and name={}",
                        entity == null ? null : entity.getType(),
                        entity == null ? null : entity.getName())))
            .flatMap(entity -> copyProperties(driver, entity))
            .flatMap(repository::update)
            .flatMap(entity -> copyProperties(entity, new AttachmentDriver()));
    }

    @Override
    public Mono<Void> removeById(UUID id) {
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
    public Mono<AttachmentDriver> findById(UUID id) {
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
    public Mono<Void> enable(UUID driverId) {
        Assert.notNull(driverId, "'driverId' must not null.");
        return repository.findById(driverId)
            .map(entity -> entity.setEnable(true))
            .flatMap(repository::update)
            .doOnSuccess(entity ->
                eventPublisher.publishEvent(new AttachmentDriverEnableEvent(this, entity)))
            .then();
    }

    @Override
    public Mono<Void> disable(UUID driverId) {
        Assert.notNull(driverId, "'driverId' must not null.");
        return repository.findById(driverId)
            .map(entity -> entity.setEnable(false))
            .flatMap(repository::update)
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
        UUID parentId = attachmentSearchCondition.getParentId();
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
    public Mono<Void> refresh(UUID attachmentId) {
        Assert.notNull(attachmentId, "'attachmentId' must not null.");
        return Mono.defer(() -> refreshTasks.computeIfAbsent(
            attachmentId, this::createRefreshTask));
    }

    private Mono<Void> createRefreshTask(UUID attachmentId) {
        return Mono.defer(() -> performRefresh(attachmentId))
            .doFinally(signalType -> refreshTasks.remove(attachmentId))
            .cache();
    }

    private Mono<Void> performRefresh(UUID attachmentId) {
        return attachmentService.findById(attachmentId)
            .switchIfEmpty(Mono.error(new AttachmentNotFoundException(
                "Attachment not found for id=" + attachmentId)))
            .flatMap(attachment -> {
                if (attachment.getType() != AttachmentType.Driver_Directory) {
                    return Mono.error(new IllegalArgumentException(
                        "Attachment is not a driver directory: " + attachmentId));
                }
                if (attachment.getDriverId() == null) {
                    return Mono.error(new IllegalStateException(
                        "Attachment driver id is missing: " + attachmentId));
                }
                return refreshRemoteFileSystem(attachment, attachment.getDriverId());
            });
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

    @Override
    public Flux<AttachmentDriverFetcherVo> listDriversFetchers() {
        return Flux.fromStream(
                extensionComponentsFinder.getExtensions(AttachmentDriverFetcher.class)
                    .stream())
            .map(fetcher -> {
                AttachmentDriverFetcherVo fetcherVo = new AttachmentDriverFetcherVo();
                fetcherVo.setName(fetcher.getDriverName());
                fetcherVo.setType(fetcher.getDriverType());
                return fetcherVo;
            });
    }

    private Mono<Void> refreshRemoteFileSystem(Attachment attachment, UUID driverId) {
        final UUID pid = attachment.getId();
        String remotePath = attachment.getFsPath();
        return repository.findById(driverId)
            .switchIfEmpty(Mono.error(new IllegalStateException(
                "Attachment driver not found for id=" + driverId)))
            .flatMap(entity -> copyProperties(entity, new AttachmentDriver()))
            .flatMap(attachmentDriver ->
                fetchAndUpdateEntities(attachmentDriver, pid, remotePath));
    }

    private Mono<Void> fetchAndUpdateEntities(
        AttachmentDriver driver, UUID pid, String remotePath) {
        AttachmentDriverFetcher fetcher =
            getAttDriverFetcher(driver.getType(), driver.getName());
        Mono<List<Attachment>> scannedAttachments =
            fetcher.getChildren(driver.getId(), pid, remotePath).collectList();
        Mono<List<AttachmentEntity>> storedAttachments = attachmentRepository
            .findAllByParentIdAndDriverId(pid, driver.getId())
            .collectList();
        return Mono.zip(scannedAttachments, storedAttachments)
            .flatMap(tuple -> synchronizeAttachments(
                fetcher, tuple.getT1(), tuple.getT2()));
    }

    private Mono<Void> synchronizeAttachments(AttachmentDriverFetcher fetcher,
                                              List<Attachment> scannedAttachments,
                                              List<AttachmentEntity> storedAttachments) {
        Map<String, AttachmentEntity> storedAttachmentMap = new HashMap<>();
        storedAttachments.forEach(attachment ->
            storedAttachmentMap.put(attachment.getFsPath(), attachment));

        Mono<Void> saveChanges = Flux.fromIterable(scannedAttachments)
            .concatMap(scannedAttachment -> {
                AttachmentEntity storedAttachment =
                    storedAttachmentMap.remove(scannedAttachment.getFsPath());
                return saveChangedAttachment(fetcher, scannedAttachment, storedAttachment);
            })
            .then();
        Mono<Void> removeMissingAttachments = Flux.fromIterable(storedAttachmentMap.values())
            .filter(attachment -> !Boolean.TRUE.equals(attachment.getDeleted()))
            .concatMap(attachment ->
                attachmentService.removeByIdOnlyRecords(attachment.getId()))
            .then();
        return saveChanges.then(removeMissingAttachments);
    }

    private Mono<Void> saveChangedAttachment(AttachmentDriverFetcher fetcher,
                                             Attachment scannedAttachment,
                                             AttachmentEntity storedAttachment) {
        boolean requiresSha1 = requiresSha1(scannedAttachment, storedAttachment);
        if (storedAttachment != null) {
            scannedAttachment.setId(storedAttachment.getId());
            if (!requiresSha1) {
                scannedAttachment.setSha1(storedAttachment.getSha1());
            }
        }
        if (!attachmentChanged(scannedAttachment, storedAttachment)) {
            return Mono.empty();
        }
        Mono<Attachment> attachmentMono = requiresSha1
            ? fetcher.calculateSha1(scannedAttachment)
            : Mono.just(scannedAttachment);
        return attachmentMono.flatMap(attachmentService::save).then();
    }

    private boolean requiresSha1(Attachment scannedAttachment,
                                 AttachmentEntity storedAttachment) {
        if (scannedAttachment.getType() != AttachmentType.Driver_File) {
            return false;
        }
        return storedAttachment == null
            || !Objects.equals(scannedAttachment.getType(), storedAttachment.getType())
            || !Objects.equals(scannedAttachment.getSize(), storedAttachment.getSize())
            || !Objects.equals(
                scannedAttachment.getModifiedTime(), storedAttachment.getModifiedTime())
            || !StringUtils.hasText(storedAttachment.getSha1());
    }

    private boolean attachmentChanged(Attachment scannedAttachment,
                                      AttachmentEntity storedAttachment) {
        return storedAttachment == null
            || Boolean.TRUE.equals(storedAttachment.getDeleted())
            || !Objects.equals(scannedAttachment.getType(), storedAttachment.getType())
            || !Objects.equals(scannedAttachment.getName(), storedAttachment.getName())
            || !Objects.equals(scannedAttachment.getPath(), storedAttachment.getPath())
            || !Objects.equals(scannedAttachment.getUrl(), storedAttachment.getUrl())
            || !Objects.equals(scannedAttachment.getFsPath(), storedAttachment.getFsPath())
            || !Objects.equals(scannedAttachment.getSize(), storedAttachment.getSize())
            || !Objects.equals(
                scannedAttachment.getModifiedTime(), storedAttachment.getModifiedTime())
            || !Objects.equals(scannedAttachment.getDriverId(), storedAttachment.getDriverId());
    }

}
