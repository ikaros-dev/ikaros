package run.ikaros.server.core.attachment.listener;

import static run.ikaros.api.core.attachment.AttachmentConst.DRIVER_URL_SPLIT_STR;
import static run.ikaros.api.core.attachment.AttachmentConst.ROOT_DIRECTORY_ID;
import static run.ikaros.api.core.attachment.AttachmentConst.ROOT_DIRECTORY_PARENT_ID;

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
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.store.entity.AttachmentDriverEntity;
import run.ikaros.server.store.repository.AttachmentDriverRepository;

@Slf4j
@Component
public class AttachmentDriverEnableListener {
    private final AttachmentService service;
    private final DynamicDirectoryResolver dynamicDirectoryResolver;
    private final AttachmentDriverRepository driverRepository;

    /**
     * Construct.
     */
    public AttachmentDriverEnableListener(AttachmentService service,
                                          DynamicDirectoryResolver dynamicDirectoryResolver,
                                          AttachmentDriverRepository driverRepository) {
        this.service = service;
        this.dynamicDirectoryResolver = dynamicDirectoryResolver;
        this.driverRepository = driverRepository;
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
        String mountName = driver.getMountName();
        if (!StringUtils.hasText(mountName)) {
            mountName = driver.getName();
        }
        if (!StringUtils.hasText(mountName)) {
            mountName = driver.getType().name();
        }

        dynamicDirectoryResolver.addDirectoryMapping(driver.getMountName(),
            driver.getRemotePath());

        return service.findByTypeAndParentIdAndName(
                AttachmentType.Driver_Directory, ROOT_DIRECTORY_PARENT_ID, mountName)
            .switchIfEmpty(Mono.just(Attachment.builder()
                .parentId(ROOT_DIRECTORY_ID)
                .type(AttachmentType.Driver_Directory)
                .name(mountName)
                .updateTime(LocalDateTime.now())
                .url(driver.getId() + DRIVER_URL_SPLIT_STR + driver.getRemotePath())
                .fsPath(driver.getRemotePath()).path("")
                .deleted(Boolean.FALSE)
                .build()))
            .map(attachment -> attachment.setDeleted(Boolean.FALSE))
            .flatMap(service::save)
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
            .map(driver -> {
                dynamicDirectoryResolver.addDirectoryMapping(driver.getMountName(),
                    driver.getRemotePath());
                return driver;
            })
            .then();
    }

}
