package run.ikaros.server.core.attachment.listener;

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
import run.ikaros.server.core.attachment.event.AttachmentDriverDisableEvent;
import run.ikaros.server.store.entity.AttachmentDriverEntity;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.repository.AttachmentRepository;

@Slf4j
@Component
public class AttachmentDriverDisableListener {
    private final AttachmentRepository repository;

    public AttachmentDriverDisableListener(AttachmentRepository repository) {
        this.repository = repository;
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
        // 路径可能是多级的，
        // 单级直接删除目录，多级需要按顺序删除目录
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
                        .map(AttachmentEntity::getId)
                        .doOnNext(id -> parentId[0] = id)
                        .flatMap(repository::deleteById)
                    ;
            })
            .then(Mono.empty());
    }

}
