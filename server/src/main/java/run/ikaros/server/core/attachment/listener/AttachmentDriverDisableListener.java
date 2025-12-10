package run.ikaros.server.core.attachment.listener;

import static run.ikaros.api.core.attachment.AttachmentConst.ROOT_DIRECTORY_ID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.config.DynamicDirectoryResolver;
import run.ikaros.server.core.attachment.event.AttachmentDriverDisableEvent;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.store.entity.AttachmentDriverEntity;

@Slf4j
@Component
public class AttachmentDriverDisableListener {
    private final AttachmentService service;
    private final DynamicDirectoryResolver dynamicDirectoryResolver;

    public AttachmentDriverDisableListener(AttachmentService service,
                                           DynamicDirectoryResolver dynamicDirectoryResolver) {
        this.service = service;
        this.dynamicDirectoryResolver = dynamicDirectoryResolver;
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

        dynamicDirectoryResolver.removeDirectoryMapping(driver.getMountName());

        return service.findByTypeAndParentIdAndName(
                AttachmentType.Driver_Directory, ROOT_DIRECTORY_ID, mountName
            ).map(Attachment::getId)
            .flatMap(service::removeByIdOnlyRecords);
    }

}
