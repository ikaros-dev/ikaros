package run.ikaros.server.core.attachment.listener;

import static run.ikaros.api.core.attachment.AttachmentConst.DRIVER_URL_SPLIT_STR;
import static run.ikaros.api.core.attachment.AttachmentConst.ROOT_DIRECTORY_ID;
import static run.ikaros.api.core.attachment.AttachmentConst.ROOT_DIRECTORY_PARENT_ID;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.core.attachment.event.AttachmentDriverEnableEvent;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.store.entity.AttachmentDriverEntity;

@Slf4j
@Component
public class AttachmentDriverEnableListener {
    private final AttachmentService service;

    public AttachmentDriverEnableListener(AttachmentService service) {
        this.service = service;
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
        // 路径可能是多级的，
        // 单级直接创建目录返回，多级需要按顺序创建目录
        String mountName = driver.getMountName();
        if (!StringUtils.hasText(mountName)) {
            mountName = driver.getName();
        }
        if (!StringUtils.hasText(mountName)) {
            mountName = driver.getType().name();
        }

        return service.findByTypeAndParentIdAndName(
                AttachmentType.Driver_Directory, ROOT_DIRECTORY_PARENT_ID, mountName)
            .switchIfEmpty(
                service.save(Attachment.builder()
                    .parentId(ROOT_DIRECTORY_ID)
                    .type(AttachmentType.Driver_Directory)
                    .name(mountName)
                    .updateTime(LocalDateTime.now())
                    .url(driver.getId() + DRIVER_URL_SPLIT_STR + driver.getRemotePath())
                    .fsPath(driver.getRemotePath()).path("")
                    .build()))
            .then();
    }

}
