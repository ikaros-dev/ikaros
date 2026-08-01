package run.ikaros.server.core.attachment.service.impl;

import static run.ikaros.api.core.attachment.AttachmentConst.DRIVER_STATIC_RESOURCE_PREFIX;
import static run.ikaros.api.core.attachment.AttachmentConst.ROOT_DIRECTORY_ID;

import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.config.DynamicDirectoryResolver;
import run.ikaros.server.core.attachment.extension.LocalAttachmentPathValidator;
import run.ikaros.server.core.attachment.service.AttachmentDriverMountService;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.store.entity.AttachmentDriverEntity;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.repository.AttachmentRepository;

/**
 * 默认附件驱动挂载服务，统一维护运行时映射和附件根目录状态.
 */
@Service
public class DefaultAttachmentDriverMountService implements AttachmentDriverMountService {
    /** 附件服务. */
    private final AttachmentService attachmentService;
    /** 附件仓库. */
    private final AttachmentRepository attachmentRepository;
    /** 动态静态资源目录解析器. */
    private final DynamicDirectoryResolver dynamicDirectoryResolver;
    /** 本地驱动路径校验器. */
    private final LocalAttachmentPathValidator pathValidator;

    /**
     * 创建默认附件驱动挂载服务.
     */
    public DefaultAttachmentDriverMountService(AttachmentService attachmentService,
                                               AttachmentRepository attachmentRepository,
                                               DynamicDirectoryResolver dynamicDirectoryResolver,
                                               LocalAttachmentPathValidator pathValidator) {
        this.attachmentService = attachmentService;
        this.attachmentRepository = attachmentRepository;
        this.dynamicDirectoryResolver = dynamicDirectoryResolver;
        this.pathValidator = pathValidator;
    }

    @Override
    public Mono<Void> mount(AttachmentDriverEntity driver) {
        Assert.notNull(driver, "Attachment driver cannot be null");
        String mountName = resolveMountName(driver);
        pathValidator.register(driver.getId(), driver.getRemotePath());
        dynamicDirectoryResolver.addDirectoryMapping(mountName, driver.getRemotePath());
        return attachmentService.findByTypeAndParentIdAndName(
                AttachmentType.Driver_Directory, ROOT_DIRECTORY_ID, mountName)
            .switchIfEmpty(Mono.just(createMountAttachment(driver, mountName)))
            .map(attachment -> updateMountAttachment(attachment, driver, mountName))
            .flatMap(attachmentService::save)
            .doOnError(exception -> {
                dynamicDirectoryResolver.removeDirectoryMapping(mountName);
                pathValidator.unregister(driver.getId());
            })
            .then();
    }

    @Override
    public Mono<Void> unmount(AttachmentDriverEntity driver) {
        Assert.notNull(driver, "Attachment driver cannot be null");
        String mountName = resolveMountName(driver);
        dynamicDirectoryResolver.removeDirectoryMapping(mountName);
        pathValidator.unregister(driver.getId());
        return attachmentRepository.findByTypeAndParentIdAndName(
                AttachmentType.Driver_Directory, ROOT_DIRECTORY_ID, mountName)
            .map(AttachmentEntity::getId)
            .flatMap(attachmentService::removeByIdOnlyRecords);
    }

    @Override
    public Mono<Void> rebind(AttachmentDriverEntity previousDriver,
                             AttachmentDriverEntity currentDriver) {
        Assert.notNull(previousDriver, "Previous attachment driver cannot be null");
        Assert.notNull(currentDriver, "Current attachment driver cannot be null");
        if (Objects.equals(resolveMountName(previousDriver), resolveMountName(currentDriver))) {
            return mount(currentDriver);
        }
        return unmount(previousDriver).then(mount(currentDriver));
    }

    private Attachment createMountAttachment(AttachmentDriverEntity driver, String mountName) {
        return Attachment.builder()
            .parentId(ROOT_DIRECTORY_ID)
            .type(AttachmentType.Driver_Directory)
            .name(mountName)
            .updateTime(LocalDateTime.now())
            .url(DRIVER_STATIC_RESOURCE_PREFIX + "/" + mountName)
            .driverId(driver.getId())
            .fsPath(driver.getRemotePath())
            .path("/" + mountName)
            .deleted(Boolean.FALSE)
            .build();
    }

    private Attachment updateMountAttachment(Attachment attachment,
                                               AttachmentDriverEntity driver,
                                               String mountName) {
        return attachment
            .setParentId(ROOT_DIRECTORY_ID)
            .setType(AttachmentType.Driver_Directory)
            .setName(mountName)
            .setUrl(DRIVER_STATIC_RESOURCE_PREFIX + "/" + mountName)
            .setDriverId(driver.getId())
            .setFsPath(driver.getRemotePath())
            .setPath("/" + mountName)
            .setDeleted(Boolean.FALSE);
    }

    private String resolveMountName(AttachmentDriverEntity driver) {
        if (StringUtils.hasText(driver.getMountName())) {
            return driver.getMountName();
        }
        if (StringUtils.hasText(driver.getName())) {
            return driver.getName();
        }
        return driver.getType().name();
    }
}
