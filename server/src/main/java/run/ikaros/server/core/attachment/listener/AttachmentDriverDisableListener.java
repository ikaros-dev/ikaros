package run.ikaros.server.core.attachment.listener;

import static run.ikaros.api.core.attachment.AttachmentConst.ROOT_DIRECTORY_ID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.config.DynamicDirectoryResolver;
import run.ikaros.server.core.attachment.event.AttachmentDriverDisableEvent;
import run.ikaros.server.core.attachment.extension.LocalAttachmentPathValidator;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.store.entity.AttachmentDriverEntity;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.repository.AttachmentRepository;

/**
 * 在本地附件驱动禁用时撤销挂载目录及其访问权限.
 */
@Slf4j
@Component
public class AttachmentDriverDisableListener {
    /** 附件仓库. */
    private final AttachmentRepository attachmentRepository;
    /** 动态静态资源目录解析器. */
    private final DynamicDirectoryResolver dynamicDirectoryResolver;
    /** 附件服务. */
    private final AttachmentService attachmentService;
    /** 本地驱动路径校验器. */
    private final LocalAttachmentPathValidator pathValidator;

    /**
     * Construct.
     */
    public AttachmentDriverDisableListener(AttachmentRepository attachmentRepository,
                                           DynamicDirectoryResolver dynamicDirectoryResolver,
                                           AttachmentService attachmentService,
                                           LocalAttachmentPathValidator pathValidator) {
        this.attachmentRepository = attachmentRepository;
        this.dynamicDirectoryResolver = dynamicDirectoryResolver;
        this.attachmentService = attachmentService;
        this.pathValidator = pathValidator;
    }

    /**
     * 移除挂载的目录对应的附件，如驱动未指定挂载路径，则默认挂载在根目录下
     * .
     */
    @EventListener(AttachmentDriverDisableEvent.class)
    public Mono<Void> onAttachmentDriverEnableEvent(AttachmentDriverDisableEvent event) {
        log.debug("Received AttachmentDriverDisableEvent: {}", event);
        Assert.notNull(event, "Attachment driver event cannot be null");
        AttachmentDriverEntity driver = event.getEntity();
        Assert.notNull(driver, "Attachment driver cannot be null");

        String mountName = driver.getMountName();
        if (!StringUtils.hasText(mountName)) {
            mountName = driver.getName();
        }
        if (!StringUtils.hasText(mountName)) {
            mountName = driver.getType().name();
        }

        dynamicDirectoryResolver.removeDirectoryMapping(mountName);
        pathValidator.unregister(driver.getId());

        return attachmentRepository.findByTypeAndParentIdAndName(
                AttachmentType.Driver_Directory, ROOT_DIRECTORY_ID, mountName
            ).map(AttachmentEntity::getId)
            .flatMap(attachmentService::removeByIdOnlyRecords);
    }

}
