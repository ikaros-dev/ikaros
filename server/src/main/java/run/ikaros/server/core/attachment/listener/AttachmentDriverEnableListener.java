package run.ikaros.server.core.attachment.listener;

import java.time.LocalDateTime;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.AttachmentConst;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.core.attachment.event.AttachmentDriverEnableEvent;
import run.ikaros.server.store.entity.AttachmentDriverEntity;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.repository.AttachmentRepository;

@Slf4j
@Component
public class AttachmentDriverEnableListener {
    private final AttachmentRepository repository;

    public AttachmentDriverEnableListener(AttachmentRepository repository) {
        this.repository = repository;
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
        String mountPath = driver.getMountPath();
        final Long[] parentId = {AttachmentConst.ROOT_DIRECTORY_ID};
        if (!StringUtils.hasText(mountPath)) {
            mountPath = driver.getName();
        }
        if (!StringUtils.hasText(mountPath)) {
            mountPath = driver.getType().name();
        }

        String[] dirNames = mountPath.split("/");

        // 过滤空串
        dirNames = Arrays.stream(dirNames)
            .filter(dir -> !dir.isEmpty())
            .toArray(String[]::new);

        if (dirNames.length == 0) {
            // 如果是空路径，直接返回根目录或null
            return Mono.empty();
        }

        // 使用 reduce 操作按顺序创建目录
        return Flux.fromArray(dirNames)
            .concatMap(dirName -> {
                // 检查目录是否已存在
                return
                    repository.findByTypeAndParentIdAndName(
                            AttachmentType.Driver, parentId[0], dirName)
                        .switchIfEmpty(
                            // 目录不存在则创建
                            repository.save(AttachmentEntity.builder()
                                .parentId(parentId[0])
                                .type(AttachmentType.Driver)
                                .name(dirName)
                                .updateTime(LocalDateTime.now())
                                .url(driver.getId() + "://" + driver.getRemotePath())
                                .fsPath("").path("")
                                .build())
                        )
                        .doOnNext(directory -> {
                            // 更新 parentId 为当前创建的目录ID，用于下一级目录创建
                            parentId[0] = directory.getId();
                        });
            })
            .last()  // 返回最后一级目录
            .then(Mono.empty());
    }

}
