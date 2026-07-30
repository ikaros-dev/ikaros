package run.ikaros.server.core.attachment.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import run.ikaros.server.core.attachment.event.AttachmentDriverDisableEvent;
import run.ikaros.server.core.attachment.service.AttachmentDriverMountService;
import run.ikaros.server.store.entity.AttachmentDriverEntity;

/**
 * 在本地附件驱动禁用时撤销挂载目录及其访问权限.
 */
@Slf4j
@Component
public class AttachmentDriverDisableListener {
    /** 附件驱动挂载服务. */
    private final AttachmentDriverMountService mountService;

    /**
     * 创建附件驱动禁用监听器.
     */
    public AttachmentDriverDisableListener(AttachmentDriverMountService mountService) {
        this.mountService = mountService;
    }

    /**
     * 卸载已禁用的附件驱动目录.
     *
     * @param event 附件驱动禁用事件
     * @return 卸载完成信号
     */
    @EventListener(AttachmentDriverDisableEvent.class)
    public Mono<Void> onAttachmentDriverEnableEvent(AttachmentDriverDisableEvent event) {
        log.debug("Received AttachmentDriverDisableEvent: {}", event);
        Assert.notNull(event, "Attachment driver event cannot be null");
        AttachmentDriverEntity driver = event.getEntity();
        Assert.notNull(driver, "Attachment driver cannot be null");
        return mountService.unmount(driver);
    }
}
