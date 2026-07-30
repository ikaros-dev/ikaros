package run.ikaros.server.core.attachment.listener;

import static run.ikaros.api.core.attachment.AttachmentConst.DRIVER_STATIC_RESOURCE_PREFIX;
import static run.ikaros.api.core.attachment.AttachmentConst.ROOT_DIRECTORY_ID;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.store.enums.AttachmentDriverType;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.config.DynamicDirectoryResolver;
import run.ikaros.server.core.attachment.event.AttachmentDriverEnableEvent;
import run.ikaros.server.core.attachment.extension.LocalAttachmentPathValidator;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.store.entity.AttachmentDriverEntity;
import run.ikaros.server.store.repository.AttachmentDriverRepository;

/**
 * 在本地附件驱动启用或应用启动时注册挂载目录.
 */
@Slf4j
@Component
public class AttachmentDriverEnableListener {
    /** 附件服务. */
    private final AttachmentService attachmentService;
    /** 动态静态资源目录解析器. */
    private final DynamicDirectoryResolver dynamicDirectoryResolver;
    /** 附件驱动仓库. */
    private final AttachmentDriverRepository driverRepository;
    /** 本地驱动路径校验器. */
    private final LocalAttachmentPathValidator pathValidator;

    /**
     * Construct.
     */
    public AttachmentDriverEnableListener(AttachmentService attachmentService,
                                          DynamicDirectoryResolver dynamicDirectoryResolver,
                                          AttachmentDriverRepository driverRepository,
                                          LocalAttachmentPathValidator pathValidator) {
        this.attachmentService = attachmentService;
        this.dynamicDirectoryResolver = dynamicDirectoryResolver;
        this.driverRepository = driverRepository;
        this.pathValidator = pathValidator;
    }

    /**
     * 添加挂载的目录对应的附件，如驱动未指定挂载路径，则默认挂载在根目录下
     * .
     */
    @EventListener(AttachmentDriverEnableEvent.class)
    public Mono<Void> onAttachmentDriverEnableEvent(AttachmentDriverEnableEvent event) {
        log.debug("Received AttachmentDriverEnableEvent: {}", event);
        Assert.notNull(event, "Attachment driver event cannot be null");
        AttachmentDriverEntity driver = event.getEntity();
        Assert.notNull(driver, "Attachment driver cannot be null");
        return mount(driver);
    }

    private Mono<Void> mount(AttachmentDriverEntity driver) {
        String mountName = driver.getMountName();
        if (!StringUtils.hasText(mountName)) {
            mountName = driver.getName();
        }
        if (!StringUtils.hasText(mountName)) {
            mountName = driver.getType().name();
        }
        final String finalMountName = mountName;

        pathValidator.register(driver.getId(), driver.getRemotePath());
        dynamicDirectoryResolver.addDirectoryMapping(finalMountName, driver.getRemotePath());

        return attachmentService.findByTypeAndParentIdAndName(
                AttachmentType.Driver_Directory, ROOT_DIRECTORY_ID, finalMountName)
            .switchIfEmpty(Mono.just(Attachment.builder()
                .parentId(ROOT_DIRECTORY_ID)
                .type(AttachmentType.Driver_Directory)
                .name(finalMountName)
                .updateTime(LocalDateTime.now())
                .url(DRIVER_STATIC_RESOURCE_PREFIX + "/" + finalMountName)
                .driverId(driver.getId())
                .fsPath(driver.getRemotePath())
                .path("/" + finalMountName)
                .deleted(Boolean.FALSE)
                .build()))
            .map(attachment -> attachment.setDeleted(Boolean.FALSE))
            .flatMap(attachmentService::save)
            .doOnError(exception -> {
                dynamicDirectoryResolver.removeDirectoryMapping(finalMountName);
                pathValidator.unregister(driver.getId());
            })
            .then();
    }

    /**
     * 初始化操作,查询启用中的附件驱动，添加静态资源映射
     * .
     */
    @EventListener(ApplicationReadyEvent.class)
    public Mono<Void> initialize() {
        return driverRepository
            .findAllByTypeAndEnable(AttachmentDriverType.LOCAL.name(), true)
            .flatMap(this::mount)
            .then();
    }

}
