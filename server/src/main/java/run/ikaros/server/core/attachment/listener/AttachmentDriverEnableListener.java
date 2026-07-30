package run.ikaros.server.core.attachment.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.AttachmentDriverType;
import run.ikaros.server.core.attachment.event.AttachmentDriverEnableEvent;
import run.ikaros.server.core.attachment.service.AttachmentDriverMountService;
import run.ikaros.server.store.entity.AttachmentDriverEntity;
import run.ikaros.server.store.repository.AttachmentDriverRepository;

/**
 * 在本地附件驱动启用或应用启动时注册挂载目录.
 */
@Slf4j
@Component
public class AttachmentDriverEnableListener {
    /** 附件驱动挂载服务. */
    private final AttachmentDriverMountService mountService;
    /** 附件驱动仓库. */
    private final AttachmentDriverRepository driverRepository;

    /**
     * 创建附件驱动启用监听器.
     */
    public AttachmentDriverEnableListener(AttachmentDriverMountService mountService,
                                          AttachmentDriverRepository driverRepository) {
        this.mountService = mountService;
        this.driverRepository = driverRepository;
    }

    /**
     * 挂载已启用的附件驱动目录.
     *
     * @param event 附件驱动启用事件
     * @return 挂载完成信号
     */
    @EventListener(AttachmentDriverEnableEvent.class)
    public Mono<Void> onAttachmentDriverEnableEvent(AttachmentDriverEnableEvent event) {
        log.debug("Received AttachmentDriverEnableEvent: {}", event);
        Assert.notNull(event, "Attachment driver event cannot be null");
        AttachmentDriverEntity driver = event.getEntity();
        Assert.notNull(driver, "Attachment driver cannot be null");
        return mountService.mount(driver);
    }

    /**
     * 初始化所有已启用的本地附件驱动挂载.
     *
     * @return 初始化完成信号
     */
    @EventListener(ApplicationReadyEvent.class)
    public Mono<Void> initialize() {
        return driverRepository
            .findAllByTypeAndEnable(AttachmentDriverType.LOCAL.name(), true)
            .flatMap(mountService::mount)
            .then();
    }
}
